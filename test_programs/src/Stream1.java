// STEAM from HPC Challenge v1.5.0 http://icl.cs.utk.edu/hpcc/
// Lines 619 - 624

class Stream1 {
    // Mmin( a_, b_ )      ( ( (a_) < (b_) ) ?  (a_) : (b_) )
    // Mmax( a_, b_ )      ( ( (a_) > (b_) ) ?  (a_) : (b_) )
    public static void main(String[] args) {
        int[] times = new int[10];
        int[] avgtime = new int[10];
        int[] maxtime = new int[10];
        int[] mintime = new int[10];
        for (int j=0; j<10; j++)
        {
            avgtime[j] = avgtime[j] + times[j];
            // mintime[j] = Mmin(mintime[j], times[j][k]);
            if(mintime[j] > times[j]) {
                mintime[j] = times[j];
            }
            // maxtime[j] = Mmax(maxtime[j], times[j][k]);
            if(maxtime[j] < times[j]) {
                maxtime[j] = times[j];
            }
        }
    }
}
