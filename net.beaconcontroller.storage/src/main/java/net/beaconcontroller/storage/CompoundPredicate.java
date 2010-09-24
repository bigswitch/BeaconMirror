package net.beaconcontroller.storage;

/** Predicate class to handle AND and OR combinations of a number
 * of child predicates. The result of the logical combination of the
 * child predicates can also be negated to support a NOT operation.
 * 
 * @author rob
 *
 */
public class CompoundPredicate implements IPredicate {

    public enum Operator { AND, OR };
    
    private Operator operator;
    private boolean negated;
    private IPredicate[] predicateList;
    
    public CompoundPredicate(Operator operator, boolean negated, IPredicate... predicateList) {
        this.operator = operator;
        this.negated = negated;
        this.predicateList = predicateList;
    }
    
    public Operator getOperator() {
        return operator;
    }
    
    public boolean isNegated() {
        return negated;
    }
    
    public IPredicate[] getPredicateList() {
        return predicateList;
    }
}
