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
public class UniformDistribution extends RandomDistribution{
        
    public UniformDistribution(double paramA, double paramB, String name, Random rng) {
        super(paramA, paramB, name, rng);
    }
    
    public double nextValue()
    {
        return paramA + (paramB - paramA) * rng.nextDouble(); 
    }
    public UniformDistribution replicate()
    {
        UniformDistribution ur = new UniformDistribution(paramA, paramB, name, rng);
        return ur;
    }
}
