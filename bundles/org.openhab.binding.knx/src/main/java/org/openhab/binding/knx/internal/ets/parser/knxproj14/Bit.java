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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * <p>
 * Java class for Bit complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Bit">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="Id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Cleared" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Set" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Width" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="Unit" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Coefficient" type="{http://www.w3.org/2001/XMLSchema}float" />
 *       &lt;attribute name="MinInclusive" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="MaxInclusive" type="{http://www.w3.org/2001/XMLSchema}int" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Bit", propOrder = { "value" })
public class Bit {

    @XmlValue
    protected java.lang.String value;
    @XmlAttribute(name = "Id")
    protected java.lang.String id;
    @XmlAttribute(name = "Cleared")
    protected java.lang.String cleared;
    @XmlAttribute(name = "Set")
    protected java.lang.String set;
    @XmlAttribute(name = "Name")
    protected java.lang.String name;
    @XmlAttribute(name = "Width")
    protected Byte width;
    @XmlAttribute(name = "Unit")
    protected java.lang.String unit;
    @XmlAttribute(name = "Coefficient")
    protected java.lang.Float coefficient;
    @XmlAttribute(name = "MinInclusive")
    protected Byte minInclusive;
    @XmlAttribute(name = "MaxInclusive")
    protected Integer maxInclusive;

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
     * Gets the value of the cleared property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getCleared() {
        return cleared;
    }

    /**
     * Sets the value of the cleared property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setCleared(java.lang.String value) {
        this.cleared = value;
    }

    /**
     * Gets the value of the set property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getSet() {
        return set;
    }

    /**
     * Sets the value of the set property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setSet(java.lang.String value) {
        this.set = value;
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
     * Gets the value of the width property.
     *
     * @return
     *         possible object is
     *         {@link Byte }
     * 
     */
    public Byte getWidth() {
        return width;
    }

    /**
     * Sets the value of the width property.
     *
     * @param value
     *            allowed object is
     *            {@link Byte }
     * 
     */
    public void setWidth(Byte value) {
        this.width = value;
    }

    /**
     * Gets the value of the unit property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getUnit() {
        return unit;
    }

    /**
     * Sets the value of the unit property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setUnit(java.lang.String value) {
        this.unit = value;
    }

    /**
     * Gets the value of the coefficient property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.Float }
     * 
     */
    public java.lang.Float getCoefficient() {
        return coefficient;
    }

    /**
     * Sets the value of the coefficient property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.Float }
     * 
     */
    public void setCoefficient(java.lang.Float value) {
        this.coefficient = value;
    }

    /**
     * Gets the value of the minInclusive property.
     *
     * @return
     *         possible object is
     *         {@link Byte }
     * 
     */
    public Byte getMinInclusive() {
        return minInclusive;
    }

    /**
     * Sets the value of the minInclusive property.
     *
     * @param value
     *            allowed object is
     *            {@link Byte }
     * 
     */
    public void setMinInclusive(Byte value) {
        this.minInclusive = value;
    }

    /**
     * Gets the value of the maxInclusive property.
     *
     * @return
     *         possible object is
     *         {@link Integer }
     * 
     */
    public Integer getMaxInclusive() {
        return maxInclusive;
    }

    /**
     * Sets the value of the maxInclusive property.
     *
     * @param value
     *            allowed object is
     *            {@link Integer }
     * 
     */
    public void setMaxInclusive(Integer value) {
        this.maxInclusive = value;
    }
}
