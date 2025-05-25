package com.alerts;

public class PriorityAlertDecorator extends AlertDecorator {
    private String priorityLevel;

    public PriorityAlertDecorator(Alert decoratedAlert, String priorityLevel) {
        super(decoratedAlert);
        this.priorityLevel = priorityLevel;
        // If BaseAlert has setPriority, we could use it:
        if (decoratedAlert instanceof BaseAlert) {
            ((BaseAlert) decoratedAlert).setPriority(priorityLevel);
        }
    }

    @Override
    public String getPriority() {
        // It could return this.priorityLevel or ensure decoratedAlert's priority is updated
        return priorityLevel;
    }

    @Override
    public String getDetails() {
        // Ensure the priority is part of the base details or append/modify here
        // The BaseAlert.getDetails() already includes priority, this decorator ensures it's set.
        // If we want to explicitly show it was overridden:
        // return decoratedAlert.getDetails().replaceFirst("Priority: \\w+", "Priority: " + priorityLevel + " (Overridden)");
        // For simplicity, we assume BaseAlert's priority is updated by the constructor.
        // Or, if BaseAlert.priority is private and no setter, then this decorator's getPriority is key.
        
        // Let's make it so this decorator *provides* the new priority,
        // and getDetails reflects that this decorator is active.
        return decoratedAlert.getDetails().replaceFirst("Priority: " + decoratedAlert.getPriority(), "Priority: " + this.priorityLevel)
               + " [Priority Decorated]";
    }
}
