namespace AtsAdvancedTest.Server
{
    using System;
    using System.Collections.Generic;
    using System.Linq;
    using System.Text;
    using System.Diagnostics;
    using System.Net.Sockets;
    using System.Net;
    using System.Threading;
    using Ace;
    using Ace.Communication;
    using Ace.Ats;

    internal class ServerProgram
    {
        /// <summary>
        /// The test action to be perfomed on established connection.
        /// </summary>
        private readonly TestAction testAction;

        private readonly ManualResetEvent completed = new ManualResetEvent(false);

        /// <summary>
        /// Event data for asynchronous accept incomming connection.
        /// </summary>
        private readonly SocketAsyncEventArgs acceptSocketEventArgs = new SocketAsyncEventArgs();

        /// <summary>
        /// Server end point.
        /// </summary>
        private readonly IPEndPoint serverEndPoint;

        /// <summary>
        /// Listening server socket.
        /// </summary>
        private readonly Socket serverSocket;

        /// <summary>
        /// Initializes a new instance of the <see cref="Server"/> class.
        /// </summary>
        /// <param name="testAction">The test action to be perfomed on established connection.</param>
        /// <param name="options">Program options.</param>
        internal ServerProgram(TestAction testAction, ProgramOptions options)
        {
            Debug.Assert(testAction != null, "Missing test action.");
            Debug.Assert(options != null, "Missing program options.");
            this.testAction = testAction;
            this.acceptSocketEventArgs.Completed += this.AcceptEventArgCompleted;
            this.serverEndPoint = new IPEndPoint(IPAddress.Any, options.Port);
            this.serverSocket = new Socket(serverEndPoint.AddressFamily, SocketType.Stream, ProtocolType.Tcp);
        }

        internal void Run()
        {
            // Starts the server such that it is listening for incoming connection requests.
            Debug.Assert(this.serverSocket != null, "Invalid operation exception: missing server socket.");
            this.serverSocket.Bind(this.serverEndPoint);
            this.serverSocket.Listen((int)SocketOptionName.MaxConnections);
            Console.Out.WriteLine("TCP Server socket is started on port {0}.", this.serverEndPoint.Port);
            this.StartAccept();

            // Wait until the program is terminated.
            Debug.Print("TCP Server is waiting forever.");
            this.completed.WaitOne();
        }

        /// <summary>
        /// Begins an operation to accept a connection request from the client.
        /// </summary>
        private void StartAccept()
        {
            this.acceptSocketEventArgs.AcceptSocket = null;
            bool willRaiseEvent = this.serverSocket.AcceptAsync(this.acceptSocketEventArgs);
            if (!willRaiseEvent)
            {
                this.ProcessAccept();
            }
        }

        /// <summary>
        /// This method is the callback method associated with Socket.AcceptAsync
        /// operations and is invoked when an accept operation is complete.
        /// </summary>
        /// <param name="sender">The sender.</param>
        /// <param name="e">The <see cref="System.Net.Sockets.SocketAsyncEventArgs"/> instance containing the event data.</param>
        private void AcceptEventArgCompleted(object sender, SocketAsyncEventArgs e)
        {
            Debug.Assert(e == this.acceptSocketEventArgs, "Unexpected event arguments when accepting incomming connection.");
            this.ProcessAccept();
        }

        /// <summary>
        /// Processes the asynchronous accept for incomming connection.
        /// </summary>
        private void ProcessAccept()
        {
            Debug.Print("TCP Server Process Accept");
            switch (this.acceptSocketEventArgs.SocketError)
            {
                case SocketError.Success:
                    // Get the socket for the accepted client and create apropriate communication channel.
                    ThreadPool.QueueUserWorkItem(this.StartAction, this.acceptSocketEventArgs.AcceptSocket);

                    // Accept the next connection request.
                    this.StartAccept();
                    break;

                case SocketError.OperationAborted:
                    // The server is stopped.
                    this.completed.Set();
                    break;

                default:
                    Program.Error(new SocketException((int)this.acceptSocketEventArgs.SocketError).Message);
                    this.completed.Set();
                    break;
            }
        }

        /// <summary>
        /// Starts the socket channel.
        /// </summary>
        /// <param name="clientSocket">The client socket.</param>
        private void StartAction(object state)
        {
            IDisposable dispose = null;
            try
            {
                Debug.Print("TCP SERVER: StartSocketChannel BEGIN");
                var clientSocket = (Socket)state;
                Debug.Assert(clientSocket != null, "Missing client socket.");
                var channel = new TcpCommunicationChannel(clientSocket);
                dispose = channel;
                Debug.Assert(this.testAction != null, "Missing test action.");
//                this.testAction(new Panel(), this.ActionCompleted, clientSocket);
            }
            catch (Exception e)
            {
                Program.Error(e.Message);
            }
#if DEBUG
            finally
            {
                Debug.Print("TCP SERVER: StartSocketChannel END");
                if (dispose != null)
                {
                    dispose.Dispose();
                }
            }
#endif
        }

        private void ActionCompleted(object state)
        {
            try
            {
                Socket clientSocket = (Socket)state;
                Debug.Assert(clientSocket != null, "Missing client socket.");
                clientSocket.Close();
            }
            catch (Exception e)
            {
                Debug.Print("Server ActionCompleted ERROR {0}", e);
                Program.Error(e.Message);
            }
        }
    }
}
