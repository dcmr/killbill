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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.killbill.billing.util.config.catalog.ValidatingConfig;
import org.killbill.billing.util.config.catalog.ValidationErrors;

@XmlRootElement(name = "stateMachineConfig")
@XmlAccessorType(XmlAccessType.NONE)
public class DefaultStateMachineConfig extends ValidatingConfig<DefaultStateMachineConfig> implements StateMachineConfig {

    @XmlElementWrapper(name = "stateMachines", required = true)
    @XmlElement(name = "stateMachine", required = true)
    private DefaultStateMachine[] stateMachines;


    @XmlElementWrapper(name = "linkStateMachines", required = true)
    @XmlElement(name = "linkStateMachine", required = true)
    private DefaultLinkStateMachine[] linkStateMachines;

    @Override
    public ValidationErrors validate(final DefaultStateMachineConfig root, final ValidationErrors errors) {
        return errors;
    }

    public DefaultStateMachine[] getStateMachines() {
        return stateMachines;
    }

    @Override
    public LinkStateMachine[] getLinkStateMachines() {
        return linkStateMachines;
    }

    public void setStateMachines(final DefaultStateMachine[] stateMachines) {
        this.stateMachines = stateMachines;
    }

    public void setLinkStateMachines(final DefaultLinkStateMachine[] linkStateMachines) {
        this.linkStateMachines = linkStateMachines;
    }
}
