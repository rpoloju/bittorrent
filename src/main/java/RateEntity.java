import java.util.concurrent.atomic.AtomicInteger;

public class RateEntity implements Comparable
{
    private int myId;
    private AtomicInteger piece_rate;

    public RateEntity(int peer_id)
    {
        myId = peer_id;
        piece_rate = new AtomicInteger();
        piece_rate.set(0);
    }

    public int get_rate() {
        return piece_rate.get();
    }

    public void reset_rate() {
        this.piece_rate.set(0);
    }

    public void increment() {
        this.piece_rate.incrementAndGet();
    }

    public int get_id() {
        return this.myId;
    }

	@Override
	public int compareTo(Object o) {
        int theirs = ((RateEntity) o).get_rate();
        
		return  theirs - get_rate();
    }
    
    @Override
    public boolean equals(Object object) {
        boolean same = false;
        if (object instanceof RateEntity) {
            same = this.myId == ((RateEntity) object).get_id();
        }

        return same;
    }
}