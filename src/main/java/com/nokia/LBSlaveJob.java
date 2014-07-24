package com.nokia;

import hudson.Extension;
import hudson.model.*;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner.PlannedNode;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA. User: miikka Date: 1/13/12 Time: 9:33 AM To change
 * this template use File | Settings | File Templates.
 */
@Extension
public class LBSlaveJob extends PeriodicWork {

    protected static final long QUARTER_MINUTE = 1000 * 15;
    protected static final long SECOND = 1000;
    private static Future<Object> provisionActivity = null;
    private static final Logger LOGGER = Logger.getLogger(LBSlaveJob.class
            .getName());

    @Override
    public long getRecurrencePeriod() {
        return SECOND * 30;
    }

    @Override
    protected void doRun() throws Exception {
        if ((provisionActivity == null ||
                provisionActivity.isDone()) &&
                !Jenkins.getInstance().isQuietingDown()) {
            LOGGER.info("Checking slave queue status...");
            provisionActivity = Computer.threadPoolForRemoting
                    .submit(new ProvisionActivity());
        }
    }
    
    private class ProvisionActivity implements Callable<Object> {
        @Override
        public Object call() throws Exception {
            final Jenkins.CloudList clouds = Jenkins.getInstance().clouds;
            LOGGER.info("Checking needed executors...");
            List<Queue.BuildableItem> buildableItems = Jenkins
                    .getInstance().getQueue().getBuildableItems();
            
            // Check how many executors are needed
            Map<Label, Integer> neededExecutors = new HashMap<Label, Integer>();
            for (Queue.BuildableItem item : buildableItems) {
                Label label = item.getAssignedLabel();
                if (neededExecutors.containsKey(label)) {
                    neededExecutors.put(label,
                                     neededExecutors.get(label)
                                             .intValue() + 1);
                } else {
                    neededExecutors.put(label, 1);
                }
            }
            
            // Try to provision required executors from load balancers
            for (Cloud cloud : clouds) {
                if (cloud instanceof LoadBalancer) {
                    if (neededExecutors.size() > 0) {
                        Iterator<Entry<Label, Integer>> i = neededExecutors.entrySet().iterator();
                        while (i.hasNext()) {
                            Entry<Label, Integer> entry = i.next();
                            Label label = entry.getKey();
                            LOGGER.info("Checking available slaves for label "
                                    + label + "...");
                            Collection<PlannedNode> slaves = cloud.provision(label,
                                         neededExecutors.get(label).intValue());
                            LOGGER.info("Received " + slaves.size() + " slaves for label " + label);
                            Integer newValue = entry.getValue().intValue() - slaves.size();
                            if (newValue > 0) {
                                entry.setValue(newValue);
                            } else {
                                i.remove();
                            }
                        }
                    }
                }
            }
            return null;
        }
        
    }
}
