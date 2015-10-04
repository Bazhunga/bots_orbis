
public class DangerTile {
	int x;
	int y;
	public DangerTile(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
	public boolean equals (int x, int y){
		if (this.x == x && this.y == y){
			return true;
		}
		else {
			return false;
		}
	}
}
