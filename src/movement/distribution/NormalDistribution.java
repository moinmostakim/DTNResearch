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
public class NormalDistribution  extends RandomDistribution{
    
    protected boolean first = true;
    private double radius, value1, value2;
    public NormalDistribution(double paramA, double paramB, String name, Random rng){
        super(paramA, paramB, name, rng);
    }
    
    @Override
    public double nextValue() {
        if (first)
        {
            double u1 = rng.nextDouble(), u2 = rng.nextDouble();
            radius = -(1.0/2.0)*Math.log(u1);
            value1 = radius * Math.cos(2 * u2);
            value2 = radius * Math.sin(2 * u2);
        }
        
        double value = (first)? value1: value2;        
        first = !first;
        return paramA + value * paramB;
    }    
    
    public NormalDistribution replicate()
    {
        return new NormalDistribution(paramA, paramB, name, rng);
    }
}
