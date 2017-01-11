package org.jenkinsci.plugins.conditionalbuildstep.lister;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.conditionalbuildstep.ConditionalRecorder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.jenkinsci.plugins.conditionalbuildstep.Messages;

import java.util.ArrayList;
import java.util.List;

public class DefaultPublisherDescriptorLister implements PublisherDescriptorLister {

    @DataBoundConstructor
    public DefaultPublisherDescriptorLister() {
    }

    public List<? extends Descriptor<? extends BuildStep>> getAllowedPublishers(AbstractProject<?,?> project) {
        final List<BuildStepDescriptor<? extends Publisher>> publishers = new ArrayList<BuildStepDescriptor<? extends Publisher>>();
        if (project == null) return publishers;
        for (Descriptor<Publisher> descriptor : Publisher.all()) {
            if (!(descriptor instanceof BuildStepDescriptor)) {
                continue;
            }
            if (descriptor instanceof ConditionalRecorder.DescriptorImpl) {
                continue;
            }

            BuildStepDescriptor<? extends Publisher> buildStepDescriptor = (BuildStepDescriptor<? extends Publisher>) descriptor;
            if (buildStepDescriptor.isApplicable(project.getClass())) {
                publishers.add(buildStepDescriptor);
            }
        }
        return publishers;
    }

    public DescriptorImpl getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<PublisherDescriptorLister> {

        @Override
        public String getDisplayName() {
            return Messages.defaultPublisherDescriptor_displayName();
        }

    }
}
