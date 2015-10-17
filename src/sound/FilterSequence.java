package sound;

/**
    The FilterSequence class is a SoundFilter that combines
    several SoundFilters at once.
    <p>This class wasn't listed in the book ;)
    @see FilteredSoundStream
*/
public class FilterSequence extends SoundFilter {

    private SoundFilter[] filters;

    /**
        Creates a new FilterSequence object with the specified
        array of SoundFilters. The samples run through each
        SoundFilter in the order of this array.
    */
    public FilterSequence(SoundFilter[] filters) {
        this.filters = filters;
    }


    /**
        Returns the maximum remaining size of all SoundFilters
        in this FilterSequence.
    */
    public int getRemainingSize() {
        int max = 0;
        for (int i=0; i<filters.length; i++) {
            max = Math.max(max, filters[i].getRemainingSize());
        }
        return max;
    }


    /**
        Resets each SoundFilter in this FilterSequence.
    */
    public void reset() {
        for (int i=0; i<filters.length; i++) {
            filters[i].reset();
        }
    }


    /**
        Filters the sound simple through each SoundFilter in this
        FilterSequence.
    */
    public void filter(byte[] samples, int offset, int length) {
        for (int i=0; i<filters.length; i++) {
            filters[i].filter(samples, offset, length);
        }
    }
}
