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
public class ParetoDistribution extends RandomDistribution{
    private static final double k = 2.0;
    //private static final double MAX = 3600.0;
    
    public ParetoDistribution(double paramA, double paramB, String name, Random rng)
    {
        super(paramA, paramB, name, rng);
    }
    public double nextValue()
    {
        return paramA / Math.pow(1 - rng.nextDouble() * (1 - Math.pow(paramA/paramB, k)), 1.0/k);                
    }
    
    public ParetoDistribution replicate()
    {
        return new ParetoDistribution(paramA, paramB, name, rng);
    }
}
