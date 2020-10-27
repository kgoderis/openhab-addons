/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
 * See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
 * Any modifications to this file will be lost upon recompilation of the source schema.
 * Generated on: 2017.03.09 at 08:34:29 PM CET
 */

package org.openhab.binding.knx.internal.ets.parser.knxproj14;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * <p>
 * Java class for Project complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Project">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice>
 *         &lt;element name="ProjectInformation" type="{http://knx.org/xml/project/14}ProjectInformation"/>
 *         &lt;element name="Installations" type="{http://knx.org/xml/project/14}Installations"/>
 *       &lt;/choice>
 *       &lt;attribute name="Id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Project", propOrder = { "projectInformation", "installations" })
public class Project {

    @XmlElement(name = "ProjectInformation")
    protected ProjectInformation projectInformation;
    @XmlElement(name = "Installations")
    protected Installations installations;
    @XmlAttribute(name = "Id")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected java.lang.String id;

    /**
     * Gets the value of the projectInformation property.
     *
     * @return
     *         possible object is
     *         {@link ProjectInformation }
     * 
     */
    public ProjectInformation getProjectInformation() {
        return projectInformation;
    }

    /**
     * Sets the value of the projectInformation property.
     *
     * @param value
     *            allowed object is
     *            {@link ProjectInformation }
     * 
     */
    public void setProjectInformation(ProjectInformation value) {
        this.projectInformation = value;
    }

    /**
     * Gets the value of the installations property.
     *
     * @return
     *         possible object is
     *         {@link Installations }
     * 
     */
    public Installations getInstallations() {
        return installations;
    }

    /**
     * Sets the value of the installations property.
     *
     * @param value
     *            allowed object is
     *            {@link Installations }
     * 
     */
    public void setInstallations(Installations value) {
        this.installations = value;
    }

    /**
     * Gets the value of the id property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setId(java.lang.String value) {
        this.id = value;
    }
}
