
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

public class CustomGameboard {
	//board state
	public StaticObject[][] staticBoard;
	public int width, height;
	
	//turret data
	public ArrayList<Turret> turrets;
	public HashMap<String, ArrayList<Turret>> turretKillZone;
	
	//player data
	public Point opponent, player;
	public Direction opponentDirection, playerDirection;

	//bullet data
	public ArrayList<Point> bulletPositions;
	public HashMap<String, Direction> bulletDirectionMap;
	public HashMap<String, Boolean> bulletSourceMap; //0 if your own, 1 if your opponents
	
	//point system
	public final int DEATH = -2500;
	public final int KILL = 2500;
	public final int HIT = 750;
	public final int POWERUP = 200;
	public final int KILL_TURRET = 500;
	public final int MOVE = 1;
	public final Direction[] directions = {Direction.LEFT, Direction.RIGHT, Direction.UP, Direction.DOWN};
	
	public enum StaticObject {
		EMPTY, WALL, POWERUP, TURRET, TELEPORTER;
	}
	
	public CustomGameboard(int width, int height, Opponent opponent, Player player,
			ArrayList<Wall> walls, ArrayList<Turret> turrets, ArrayList<PowerUp> powerUps,
			ArrayList<Point> teleportLocations, ArrayList<Bullet> bullets) {
		this.width = width;
		this.height = height;
		this.staticBoard = new StaticObject[width][height];
		this.player = new Point(player.getX(), player.getY());
		this.opponent = new Point(opponent.getX(), opponent.getY());
		this.playerDirection = player.getDirection();
		this.opponentDirection = opponent.getDirection();
		this.turretKillZone = new HashMap<String, ArrayList<Turret>>();
		
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				staticBoard[i][j] = StaticObject.EMPTY;
			}
		}
		for (Wall wall : walls) {
			staticBoard[wall.getX()][wall.getY()] = StaticObject.WALL;
		}
		this.turrets = turrets;
		for (Turret turret : turrets) {
			staticBoard[turret.getX()][turret.getY()] = StaticObject.TURRET;
			//generate turret kill map
			//left 
			String key;
			int distance;
			Point turretPoint = new Point(turret.getX(), turret.getY());
			Point tempPoint;
			
			//generate turret map for each direction for each turret
			for (Direction d : directions) {
				distance = 0;
				tempPoint = movePoint(new Point(turretPoint), d);
				while(!isSolid(tempPoint) && distance <= 4) {
					key = tempPoint.x + " " + tempPoint.y;
					
					if (turretKillZone.get(key) == null) {
						turretKillZone.put(key, new ArrayList<Turret>());
					}
					turretKillZone.get(key).add(turret);
					
					movePoint(tempPoint, d);
					distance++;
				}
			}
		}
		for (PowerUp powerUp : powerUps) {
			staticBoard[powerUp.getX()][powerUp.getY()] = StaticObject.POWERUP;
		}
		for (Point teleportLocation : teleportLocations) {
			staticBoard[teleportLocation.x][teleportLocation.y] = StaticObject.TELEPORTER;
		}
		for (Bullet bullet : bullets) {
			this.bulletPositions.add(new Point(bullet.getX(), bullet.getY()));
			this.bulletDirectionMap.put(bullet.getX() + " " + bullet.getY(), bullet.getDirection());
			this.bulletSourceMap.put(bullet.getX() + " " + bullet.getY(), bullet.getShooter() == opponent);
		}
	}
	
	public int updateBoard(Move move, int turn) {
		Point newPlayer = new Point(player);
		if (move == Move.FORWARD) {
			newPlayer = movePoint(newPlayer, playerDirection);
			//check for death or illegal moves
			if (isSolid(newPlayer))
				return DEATH;
			
			//bullet and player collide head on
			if (bulletDirectionMap.get(newPlayer.x + " " + newPlayer.y) == opposite(playerDirection))
				return DEATH;
		}
		
		//Process turrets and bullet movement + deaths
		
		for (Turret turret : turrets) {
			//if ()
		}
		
		return 0;
	}
	
	public boolean isTurretOn(Turret t, int turn) {
		int ft = t.getFireTime();
		int ct = t.getCooldownTime();
		
		return turn % (ft + ct) <= ft;
	}
	
	Direction opposite(Direction d) {
		if (d == Direction.DOWN) return Direction.UP;
		if (d == Direction.LEFT) return Direction.RIGHT;
		if (d == Direction.RIGHT) return Direction.LEFT;
		return Direction.DOWN;
	}
	
	public Point movePoint(Point p, Direction d) {
		if (d == Direction.UP) {
			p.y -= 1;
		} else if (d == Direction.RIGHT) {
			p.x += 1;
		} else if (d == Direction.DOWN) {
			p.y += 1;
		} else { //left
			p.x -= 1;
		}
		return wrapCoordinate(p);
	}
	
	public Point wrapCoordinate(Point p) {
		while (p.x < 0) p.x += width;
		while (p.y < 0) p.y += height;
				
		p.x %= width;
		p.y %= height;
		
		return p;
	}
	
	public Point wrapCoordinate(int x, int y) {
		return wrapCoordinate(new Point(x, y));
	}
	
	public boolean isSolid(Point p) {
		if (staticBoard[p.x][p.y] == StaticObject.WALL || staticBoard[p.x][p.y] == StaticObject.TURRET)
			return true;
		return false;
	}
	
}
