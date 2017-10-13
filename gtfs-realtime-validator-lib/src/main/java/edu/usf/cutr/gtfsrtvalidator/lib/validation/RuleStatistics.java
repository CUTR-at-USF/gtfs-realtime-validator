package edu.usf.cutr.gtfsrtvalidator.lib.validation;

public class RuleStatistics {

    private double ruleExecutionTime;
    private String ruleId;

    public RuleStatistics() {
    }

    /**
     * Returns the amount of time it took to execute the rule specified by the getRuleId() method on this object, in seconds as a decimal (0.22)
     *
     * @return the amount of time it took to execute the rule specified by the getRuleId() method on this object, in seconds as a decimal (0.22)
     */
    public double getRuleExecutionTime() {
        return ruleExecutionTime;
    }

    /**
     * Sets the amount of time it took to execute the rule specified by the getRuleId() method on this object, in seconds as a decimal (0.22)
     *
     * @param ruleExecutionTime the amount of time it took to execute the rule specified by the getRuleId() method on this object, in seconds as a decimal (0.22)
     */
    public void setRuleExecutionTime(double ruleExecutionTime) {
        this.ruleExecutionTime = ruleExecutionTime;
    }

    /**
     * Returns the validator class name for which this ruleExecutionTime was recorded
     *
     * @return the validator class name for which this ruleExecutionTime was recorded
     */
    public String getValidator() {
        return ruleId;
    }

    /**
     * Sets the validator class name for which this ruleExecutionTime was recorded
     *
     * @param ruleId the validator class name for which this ruleExecutionTime was recorded
     */
    public void setValidator(String ruleId) {
        this.ruleId = ruleId;
    }

    @Override
    public String toString() {
        return "RuleStatistics{" +
                "ruleExecutionTime=" + ruleExecutionTime +
                ", ruleId='" + ruleId + '\'' +
                '}';
    }
}
