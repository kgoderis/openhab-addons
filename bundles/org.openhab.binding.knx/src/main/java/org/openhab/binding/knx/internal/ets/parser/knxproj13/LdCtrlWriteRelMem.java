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
 * Java class for LdCtrlWriteRelMem complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="LdCtrlWriteRelMem">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="ObjIdx" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="Offset" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="Size" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="Verify" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Obj" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="Occurrence" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LdCtrlWriteRelMem", propOrder = { "value" })
public class LdCtrlWriteRelMem {

    @XmlValue
    protected java.lang.String value;
    @XmlAttribute(name = "ObjIdx")
    protected Byte objIdx;
    @XmlAttribute(name = "Offset")
    protected Byte offset;
    @XmlAttribute(name = "Size")
    protected Integer size;
    @XmlAttribute(name = "Verify")
    protected java.lang.String verify;
    @XmlAttribute(name = "Obj")
    protected Byte obj;
    @XmlAttribute(name = "Occurrence")
    protected Byte occurrence;

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
     * Gets the value of the objIdx property.
     *
     * @return
     *         possible object is
     *         {@link Byte }
     * 
     */
    public Byte getObjIdx() {
        return objIdx;
    }

    /**
     * Sets the value of the objIdx property.
     *
     * @param value
     *            allowed object is
     *            {@link Byte }
     * 
     */
    public void setObjIdx(Byte value) {
        this.objIdx = value;
    }

    /**
     * Gets the value of the offset property.
     *
     * @return
     *         possible object is
     *         {@link Byte }
     * 
     */
    public Byte getOffset() {
        return offset;
    }

    /**
     * Sets the value of the offset property.
     *
     * @param value
     *            allowed object is
     *            {@link Byte }
     * 
     */
    public void setOffset(Byte value) {
        this.offset = value;
    }

    /**
     * Gets the value of the size property.
     *
     * @return
     *         possible object is
     *         {@link Integer }
     * 
     */
    public Integer getSize() {
        return size;
    }

    /**
     * Sets the value of the size property.
     *
     * @param value
     *            allowed object is
     *            {@link Integer }
     * 
     */
    public void setSize(Integer value) {
        this.size = value;
    }

    /**
     * Gets the value of the verify property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getVerify() {
        return verify;
    }

    /**
     * Sets the value of the verify property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setVerify(java.lang.String value) {
        this.verify = value;
    }

    /**
     * Gets the value of the obj property.
     *
     * @return
     *         possible object is
     *         {@link Byte }
     * 
     */
    public Byte getObj() {
        return obj;
    }

    /**
     * Sets the value of the obj property.
     *
     * @param value
     *            allowed object is
     *            {@link Byte }
     * 
     */
    public void setObj(Byte value) {
        this.obj = value;
    }

    /**
     * Gets the value of the occurrence property.
     *
     * @return
     *         possible object is
     *         {@link Byte }
     * 
     */
    public Byte getOccurrence() {
        return occurrence;
    }

    /**
     * Sets the value of the occurrence property.
     *
     * @param value
     *            allowed object is
     *            {@link Byte }
     * 
     */
    public void setOccurrence(Byte value) {
        this.occurrence = value;
    }
}
