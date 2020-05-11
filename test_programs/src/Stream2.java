// STEAM from HPC Challenge v1.5.0 http://icl.cs.utk.edu/hpcc/
// Lines 629 - 647

class Stream2 {
    // Mmin( a_, b_ )      ( ( (a_) < (b_) ) ?  (a_) : (b_) )
    // Mmax( a_, b_ )      ( ( (a_) > (b_) ) ?  (a_) : (b_) )
    public static void main(String[] args) {
        int NTIMES = 243;
        int[] avgtime = new int[10];
        int[] bytes = new int[10];
        int[] mintime = new int[10];
        int array_elements = 10;
        int curGBs = 1;
        for (int j = 0; j < 4; j++) {
            avgtime[j] = avgtime[j] / (NTIMES - 1); /* note -- skip first iteration */
            /* make sure no division by zero */
            // curGBs = (mintime[j] > 0.0 ? 1.0 / mintime[j] : -1.0);
            if(mintime[j] > 0) {
                mintime[j] = 1 / mintime[j];
            } else {
                mintime[j] = -1;
            }
            curGBs *= 1e-9 * bytes[j] * array_elements;
//            if (doIO)
//                fprintf(outFile, "%s%11.4f  %11.4f  %11.4f  %11.4f\n", label[j],
//                        curGBs,
//                        avgtime[j],
//                        mintime[j],
//                        maxtime[j]);
//            switch (j) {
//                case 0: *copyGBs = curGBs;
//                    break;
//                case 1: *scaleGBs = curGBs;
//                    break;
//                case 2: *addGBs = curGBs;
//                    break;
//                case 3: *triadGBs = curGBs;
//                    break;
//            }
        }
    }
}
