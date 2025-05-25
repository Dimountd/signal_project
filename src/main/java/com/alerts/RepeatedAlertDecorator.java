package com.alerts;

public class RepeatedAlertDecorator extends AlertDecorator {

    public RepeatedAlertDecorator(Alert decoratedAlert) {
        super(decoratedAlert);
        this.decoratedAlert.setRepeated(true);
    }

    @Override
    public boolean isRepeated() {
        return true; // This decorator marks it as repeated
    }

    @Override
    public String getDetails() {
        // Ensure the repeated status is part of the base details or append/modify here
        // BaseAlert.getDetails() already includes "(Repeated)" if set.
        // This decorator ensures the flag is set on the decorated alert.
        // If we want to explicitly show it was decorated as repeated:
        // return decoratedAlert.getDetails() + " [Marked as Repeated by Decorator]";
        // Since BaseAlert handles the "(Repeated)" text, just ensuring the flag is set is enough.
        return decoratedAlert.getDetails(); // The flag is set in constructor
    }
}
