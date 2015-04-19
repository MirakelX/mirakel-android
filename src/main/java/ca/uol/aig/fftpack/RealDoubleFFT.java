package ca.uol.aig.fftpack;
/**
 * FFT transform of a real periodic sequence.
 * @author Baoshe Zhang
 * @author Astronomical Instrument Group of University of Lethbridge.
 */
public class RealDoubleFFT extends RealDoubleFFT_Mixed
{
    /**
     * <em>norm_factor</em> can be used to normalize this FFT transform. This is because
     * a call of forward transform (<em>ft</em>) followed by a call of backward transform
     * (<em>bt</em>) will multiply the input sequence by <em>norm_factor</em>.
     */
    public double norm_factor;
    private double wavetable[];
    private int ndim;

    /**
     * Construct a wavenumber table with size <em>n</em>.
     * The sequences with the same size can share a wavenumber table. The prime
     * factorization of <em>n</em> together with a tabulation of the trigonometric functions
     * are computed and stored.
     *
     * @param  n  the size of a real data sequence. When <em>n</em> is a multiplication of small
     * numbers (4, 2, 3, 5), this FFT transform is very efficient.
     */
    public RealDoubleFFT(int n)
    {
        ndim = n;
        norm_factor = n;
        if(wavetable == null || wavetable.length !=(2*ndim+15))
        {
            wavetable = new double[2*ndim + 15];
        }
        rffti(ndim, wavetable);
    }

    /**
     * Forward real FFT transform. It computes the discrete transform of a real data sequence.
     *
     * @param x an array which contains the sequence to be transformed. After FFT,
     * <em>x</em> contains the transform coeffients used to construct <em>n</em> complex FFT coeffients.
     * <br>
     * The real part of the first complex FFT coeffients is <em>x</em>[0]; its imaginary part
     * is 0. If <em>n</em> is even set <em>m</em> = <em>n</em>/2, if <em>n</em> is odd set
     * <em>m</em> = <em>n</em>/2, then for
     * <br>
     * <em>k</em> = 1, ..., <em>m</em>-1 <br>
     * the real part of <em>k</em>-th complex FFT coeffients is <em>x</em>[2*<em>k</em>-1];
     * <br>
     * the imaginary part of <em>k</em>-th complex FFT coeffients is <em>x</em>[2*<em>k</em>-2].
     * <br>
     * If <em>n</em> is even,
     * the real of part of (<em>n</em>/2)-th complex FFT coeffients is <em>x</em>[<em>n</em>]; its imaginary part is 0.
     * The remaining complex FFT coeffients can be obtained by the symmetry relation:
     * the (<em>n</em>-<em>k</em>)-th complex FFT coeffient is the conjugate of <em>n</em>-th complex FFT coeffient.
     *
     */
    public void ft(double x[])
    {
        if(x.length != ndim)
            throw new IllegalArgumentException("The length of data can not match that of the wavetable");
        rfftf(ndim, x, wavetable);
    }

    /**
     * Forward real FFT transform. It computes the discrete transform of a real data sequence.
     *
     * @param x an array which contains the sequence to be transformed. After FFT,
     * <em>x</em> contains the transform coeffients used to construct <em>n</em> complex FFT coeffients.
     * <br>
     * @param y the first complex (<em>n</em>+1)/2 (when <em>n</em> is odd) or (<em>n</em>/2+1) (when
     * <em>n</em> is even) FFT coeffients.
     * The remaining complex FFT coeffients can be obtained by the symmetry relation:
     * the (<em>n</em>-<em>k</em>)-th complex FFT coeffient is the conjugate of <em>n</em>-th complex FFT coeffient.
     *
     */
    public void ft(double x[], Complex1D y)
    {
        if(x.length != ndim)
            throw new IllegalArgumentException("The length of data can not match that of the wavetable");
        rfftf(ndim, x, wavetable);

        if(ndim%2 == 0)
        {
            y.x = new double[ndim/2 + 1];
            y.y = new double[ndim/2 + 1];
        }
        else
        {
            y.x = new double[(ndim+1)/2];
            y.y = new double[(ndim+1)/2];
        }


        y.x[0] = x[0];
        y.y[0] = 0.0D;
        for(int i=1; i<(ndim+1)/2; i++)
        {
            y.x[i] = x[2*i-1];
            y.y[i] = x[2*i];
        }
        if(ndim%2 == 0)
        {
            y.x[ndim/2] = x[ndim-1];
            y.y[ndim/2] = 0.0D;
        }

    }

    /**
     * Backward real FFT transform. It is the unnormalized inverse transform of <em>ft</em>(double[]).
     *
     * @param x an array which contains the sequence to be transformed. After FFT,
     * <em>x</em> contains the transform coeffients. Also see the comments of <em>ft</em>(double[])
     * for the relation between <em>x</em> and complex FFT coeffients.
     */
    public void bt(double x[])
    {
        if(x.length != ndim)
            throw new IllegalArgumentException("The length of data can not match that of the wavetable");
        rfftb(ndim, x, wavetable);
    }
    /**
     * Backward real FFT transform. It is the unnormalized inverse transform of <em>ft</em>(Complex1D, double[]).
     *
     * @param x  an array which contains the sequence to be transformed. When <em>n</em> is odd, it contains the first
     * (<em>n</em>+1)/2 complex data; when <em>n</em> is even, it contains (<em>n</em>/2+1) complex data.
     * @param y  the real FFT coeffients.
     * <br>
     * Also see the comments of <em>ft</em>(double[]) for the relation
     * between <em>x</em> and complex FFT coeffients.
     */
    public void bt(Complex1D x, double y[])
    {
        if(ndim%2 == 0)
        {
            if(x.x.length != ndim/2+1)
                throw new IllegalArgumentException("The length of data can not match that of the wavetable");
        }
        else
        {
            if(x.x.length != (ndim+1)/2)
                throw new IllegalArgumentException("The length of data can not match that of the wavetable");
        }

        y[0] = x.x[0];
        for(int i=1; i<(ndim+1)/2; i++)
        {
            y[2*i-1]=x.x[i];
            y[2*i]=x.y[i];
        }
        if(ndim%2 == 0)
        {
            y[ndim-1]=x.x[ndim/2];
        }
        rfftb(ndim, y, wavetable);
    }
}
