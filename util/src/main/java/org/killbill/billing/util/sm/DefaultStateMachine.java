/*
 * Copyright 2014 Groupon, Inc
 *
 * Groupon licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.util.sm;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

import org.killbill.billing.util.config.catalog.ValidatingConfig;
import org.killbill.billing.util.config.catalog.ValidationErrors;

@XmlAccessorType(XmlAccessType.NONE)
public class DefaultStateMachine extends ValidatingConfig<DefaultStateMachineConfig> implements StateMachine {

    @XmlAttribute(required = true)
    @XmlID
    private String name;

    @XmlElementWrapper(name = "states", required = true)
    @XmlElement(name = "state", required = true)
    private DefaultState[] states;

    @XmlElementWrapper(name = "transitions", required = true)
    @XmlElement(name = "transition", required = true)
    private DefaultTransition[] transitions;

    @XmlElementWrapper(name = "operations", required = true)
    @XmlElement(name = "operation", required = true)
    private DefaultOperation[] operations;

    @Override
    public ValidationErrors validate(final DefaultStateMachineConfig root, final ValidationErrors errors) {
        return errors;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public State[] getStates() {
        return states;
    }

    @Override
    public Transition[] getTransitions() {
        return transitions;
    }

    @Override
    public Operation[] getOperations() {
        return operations;
    }

    public void setStates(final DefaultState[] states) {
        this.states = states;
    }

    public void setTransitions(final DefaultTransition[] transitions) {
        this.transitions = transitions;
    }

    public void setOperations(final DefaultOperation[] operations) {
        this.operations = operations;
    }
}

