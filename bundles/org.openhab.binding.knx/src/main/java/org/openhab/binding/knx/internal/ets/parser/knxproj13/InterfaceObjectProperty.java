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

package org.openhab.binding.knx.internal.ets.parser.knxproj13;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * <p>
 * Java class for InterfaceObjectProperty complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="InterfaceObjectProperty">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="Id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Number" type="{http://www.w3.org/2001/XMLSchema}short" />
 *       &lt;attribute name="Name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Text" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="PDT" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Array" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Object" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="DPT" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InterfaceObjectProperty", propOrder = { "value" })
public class InterfaceObjectProperty {

    @XmlValue
    protected java.lang.String value;
    @XmlAttribute(name = "Id")
    protected java.lang.String id;
    @XmlAttribute(name = "Number")
    protected Short number;
    @XmlAttribute(name = "Name")
    protected java.lang.String name;
    @XmlAttribute(name = "Text")
    protected java.lang.String text;
    @XmlAttribute(name = "PDT")
    protected java.lang.String pdt;
    @XmlAttribute(name = "Array")
    protected java.lang.String array;
    @XmlAttribute(name = "Object")
    protected java.lang.String object;
    @XmlAttribute(name = "DPT")
    protected java.lang.String dpt;

    /**
     * Gets the value of the value property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setValue(java.lang.String value) {
        this.value = value;
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

    /**
     * Gets the value of the number property.
     *
     * @return
     *         possible object is
     *         {@link Short }
     * 
     */
    public Short getNumber() {
        return number;
    }

    /**
     * Sets the value of the number property.
     *
     * @param value
     *            allowed object is
     *            {@link Short }
     * 
     */
    public void setNumber(Short value) {
        this.number = value;
    }

    /**
     * Gets the value of the name property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setName(java.lang.String value) {
        this.name = value;
    }

    /**
     * Gets the value of the text property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getText() {
        return text;
    }

    /**
     * Sets the value of the text property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setText(java.lang.String value) {
        this.text = value;
    }

    /**
     * Gets the value of the pdt property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getPDT() {
        return pdt;
    }

    /**
     * Sets the value of the pdt property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setPDT(java.lang.String value) {
        this.pdt = value;
    }

    /**
     * Gets the value of the array property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getArray() {
        return array;
    }

    /**
     * Sets the value of the array property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setArray(java.lang.String value) {
        this.array = value;
    }

    /**
     * Gets the value of the object property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getObject() {
        return object;
    }

    /**
     * Sets the value of the object property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setObject(java.lang.String value) {
        this.object = value;
    }

    /**
     * Gets the value of the dpt property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getDPT() {
        return dpt;
    }

    /**
     * Sets the value of the dpt property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setDPT(java.lang.String value) {
        this.dpt = value;
    }
}
