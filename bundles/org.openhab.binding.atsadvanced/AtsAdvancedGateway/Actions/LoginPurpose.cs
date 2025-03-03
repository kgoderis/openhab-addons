namespace AtsAdvancedTest.Actions
{
    using System;

    /// <summary>
    /// Flags indicating the purpose of the remote connection to the device.
    /// </summary>
    [Flags]
    internal enum LoginPurpose
    {
        /// <summary>
        /// Placeholder for uninitialized value.
        /// </summary>
        None = 0,

        /// <summary>
        /// Flag indicating whether the operator shall be able to upload device configuration.
        /// </summary>
        Upload = 1,

        /// <summary>
        /// Flag indicating whether the operator shall be able to download device configuration.
        /// </summary>
        Download = 2,

        /// <summary>
        /// Flag indicating whether the operator shall be able to remotely control the device.
        /// </summary>
        Control = 4,

        /// <summary>
        /// Flag indicating whether the operator shall be able to remotely monitor the device.
        /// </summary>
        Monitor = 8,

        /// <summary>
        /// Flag indicating whether the operator shall be able to remotely retrieve diagnostics data from the device.
        /// </summary>
        Diagnostics = 16,

        /// <summary>
        /// Flag indicating whether the operator shall be able to remotely retrieve logged data from the device.
        /// </summary>
        LogView = 32,
    }
}
