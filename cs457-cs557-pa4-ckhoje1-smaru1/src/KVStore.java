public class KVStore {

	//  private Map<Integer, String> store = new HashMap<Integer, String>();
	private int key = 999;
	private String value = null;
	private long time = 0;
	private int MIN_KEY_VALUE = 0;
	private int MAX_KEY_VALUE = 255;

	public KVStore() { }

	public int getKey() {
		return key;
	}

	public void setKey(int key) { this.key = key; }

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public int getMIN_KEY_VALUE() {
		return MIN_KEY_VALUE;
	}

	public void setMIN_KEY_VALUE(int MIN_KEY_VALUE) {
		this.MIN_KEY_VALUE = MIN_KEY_VALUE;
	}

	public int getMAX_KEY_VALUE() {
		return MAX_KEY_VALUE;
	}

	public void setMAX_KEY_VALUE(int MAX_KEY_VALUE) {
		this.MAX_KEY_VALUE = MAX_KEY_VALUE;
	}
}
