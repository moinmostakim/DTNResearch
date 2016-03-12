/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package movement.distribution;

import java.util.Random;

/**
 *
 * @author Yusuf Sarwar
 */
public abstract class RandomDistribution {
    protected double paramA;
    protected double paramB;
    protected String name;
    protected Random rng;
    
    public RandomDistribution(double paramA, double paramB, String name, Random rng) {
        this.paramA = paramA;
        this.paramB = paramB;
        this.name = name;
        this.rng = rng;
    }
    
    public abstract double nextValue();
    public abstract RandomDistribution replicate();
}
