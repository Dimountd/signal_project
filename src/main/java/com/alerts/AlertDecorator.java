package com.alerts;

public abstract class AlertDecorator implements Alert {
    protected Alert decoratedAlert;

    public AlertDecorator(Alert decoratedAlert) {
        this.decoratedAlert = decoratedAlert;
    }

    @Override
    public String getPatientId() {
        return decoratedAlert.getPatientId();
    }

    @Override
    public String getCondition() {
        return decoratedAlert.getCondition();
    }

    @Override
    public long getTimestamp() {
        return decoratedAlert.getTimestamp();
    }

    @Override
    public String getPriority() {
        return decoratedAlert.getPriority();
    }

    @Override
    public String getDetails() {
        return decoratedAlert.getDetails();
    }

    @Override
    public boolean isRepeated() {
        return decoratedAlert.isRepeated();
    }

    @Override
    public void setRepeated(boolean repeated) {
        decoratedAlert.setRepeated(repeated);
    }
}
