/*
 * The MIT License
 *
 * Copyright (C) 2011 by Dominik Bartholdi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.conditionalbuildstep;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.DependecyDeclarer;
import hudson.model.DependencyGraph;
import hudson.model.Descriptor;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import net.sf.json.JSONObject;
import org.jenkins_ci.plugins.run_condition.RunCondition;
import org.jenkinsci.plugins.conditionalbuildstep.lister.DefaultPublisherDescriptorLister;
import org.jenkinsci.plugins.conditionalbuildstep.lister.PublisherDescriptorLister;
import org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A post buildstep wrapping any number of other buildsteps, controlling their post execution based on a defined condition.
 *
 * @author Phil Madden (philmadden83)
 */
public class ConditionalRecorder extends Recorder {
    private RunCondition runCondition;
    private List<BuildStep> conditionalPublishers;

    @DataBoundConstructor
    public ConditionalRecorder(RunCondition runCondition, List<BuildStep> conditionalPublishers) {
        this.runCondition = runCondition;
        this.conditionalPublishers = conditionalPublishers;
    }

    public RunCondition getRunCondition() {
        return runCondition;
    }
    
    @Override
    public Collection getProjectActions(AbstractProject<?, ?> project) {
        final Collection projectActions = new ArrayList();
        for (BuildStep buildStep : getConditionalPublishers()) {
            Collection<? extends Action> pas = buildStep.getProjectActions(project);
            if(pas != null) {
                projectActions.addAll(pas);
            }
        }
        return projectActions;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
    public List<BuildStep> getConditionalPublishers() {
        if(conditionalPublishers == null) {
            conditionalPublishers = new ArrayList<BuildStep>();
        }
        return conditionalPublishers;
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {
        try {
            if (runCondition.runPerform(build, listener)) {
                return new BuilderChain(getConditionalPublishers()).perform(build, launcher, listener);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private final PublisherDescriptorLister publisherDescriptorLister;

        public DescriptorImpl() {
            this.publisherDescriptorLister = new DefaultPublisherDescriptorLister();
        }

        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            // No need for aggregation for matrix build with MatrixAggregatable
            // this is only supported for: {@link Publisher}, {@link JobProperty}, {@link BuildWrapper}
            return !SingleConditionalBuilder.PROMOTION_JOB_TYPE.equals(aClass.getCanonicalName());
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return Messages.multistepbuilder_displayName();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

        public List<? extends Descriptor<? extends BuildStep>> getBuilderDescriptors(AbstractProject<?, ?> project) {
           return  publisherDescriptorLister.getAllowedPublishers(project);
        }

        public List<? extends Descriptor<? extends RunCondition>> getRunConditions() {
            return RunCondition.all();
        }

    }

    public void buildDependencyGraph(AbstractProject project, DependencyGraph graph) {
        for (BuildStep builder : getConditionalPublishers()) {
            if(builder instanceof DependecyDeclarer) {
                ((DependecyDeclarer)builder).buildDependencyGraph(project, graph);
            }
        }
    }

}
