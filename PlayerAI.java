import java.util.*;
import java.awt.Point;

public class PlayerAI extends ClientAI {
	int totalTurns;
	public PlayerAI() {
		//Write your initialization here
		totalTurns = -1;
	}

	@Override
	public Move getMove(Gameboard gameboard, Opponent opponent, Player player) throws NoItemException, MapOutOfBoundsException {
		if (totalTurns == -1){
			// Initial state of the board
			totalTurns = gameboard.getTurnsRemaining();
		}
		int x = player.getX();
		int y = player.getY();
		Direction d = player.getDirection();
		Move mv = Move.NONE;
		if (isWallAhead(d, gameboard, x, y) || isBulletDangerAhead(d, gameboard, x, y) || isTurretDangerAhead(d, gameboard, x, y)){
			System.out.println("OH SHIT");
			mv = turnLeft(d);
		}
		else {
			mv = Move.FORWARD;
		}
		return mv;
	}
	
	public Move turnLeft(Direction d){
		if (d.equals(Direction.UP)){
			return Move.FACE_LEFT;
		}
		else if (d.equals(Direction.RIGHT)){
			return Move.FACE_UP;
		}
		else if (d.equals(Direction.DOWN)){
			return Move.FACE_RIGHT;
		}
		else{
			return Move.FACE_DOWN;
		}	
	}
	
	
	public boolean[] checkDangers(Gameboard gameboard, Player player){
		// List of Dangers/Places one cannot go
		// Obvious
		// 1. Into a wall
		// 2. Into a turret laser
		// 3. Staying on a tile and getting lasered
		// Opponent Dependent
		// 1. Into their bullets
		// 2. Moving into a possible laser
		// 3. 
		// Will be true if danger lies in certain direction, false otherwise
		// The player will be forced to take a hit if a projectile or laser will move onto their
		// square and the dodging direction is not forward.
		// Therefore we have to first check at least 2 squares ahead for an incoming bullet so we can pivot and dodge
		// We have to check if there are any turrets in the column and row of the place we want to move that are firing
		// in the next 2 turns
		// 
		int x = player.getX();
		int y = player.getY();
		boolean[] dangers = new boolean[5]; //Clockwise directions -> up/right/down/left/current
		
		// Check the direction
		Direction d = player.getDirection();
		
		
		
		return dangers;
	}
	
	public boolean isWallAhead(Direction d, Gameboard gameboard, int x, int y) throws MapOutOfBoundsException{
		int moveX = 0, moveY = 0;
		moveX = getMoveX(d);
		moveY = getMoveY(d);
		for (int i = 0; i < 1; i++){
			int destX = 0, destY = 0;
			destX = handleWrapAroundX(x, moveX, gameboard);
			destY = handleWrapAroundY(y, moveY, gameboard);
			if (gameboard.isWallAtTile(destX, destY)){
				return true;
			}
			return false;
		}
		return false;
	}
	
	// x and y are current coordinates. 
	// moveX and moveY is the direction we should check for dangers
	public boolean isBulletDangerAhead(Direction d, Gameboard gameboard, int x, int y) throws MapOutOfBoundsException{
		int moveX = 0 ,moveY = 0;
		moveX = getMoveX(d);
		moveY = getMoveY(d);
		for (int i = 0; i < 3; i++){
			int destX = 0, destY = 0;
			destX = handleWrapAroundX(x, moveX, gameboard);
			destY = handleWrapAroundY(y, moveY, gameboard);

			// Handle bullets
			if (gameboard.areBulletsAtTile(destX, destY)){
				return true;
			}
		}
		return false;
	}
	
	public boolean isTurretDangerAhead(Direction d, Gameboard gameboard, int x, int y) throws MapOutOfBoundsException{
		int moveX = 0, moveY = 0;
		moveX = getMoveX(d);
		moveY = getMoveY(d);
		for (int i = 1; i < 5; i++){
			for (int j = -4; j < 5; j++){
				int destX = 0, destY = 0;
				destX = handleWrapAroundX(x, moveX*i, gameboard);
				destY = handleWrapAroundY(y, moveY*j, gameboard);
				System.out.println("t_DestX: " + destX + " t_destY " + destY);
				try {
					Turret trt = gameboard.getTurretAtTile(destX, destY);
					System.out.println("TURRET FOUND AT " + destX + ", " + destY);
					if (trt.isFiringNextTurn()){
						return true;
					}
					// The turret is firing if the current turn # mod (cooldowntime + firetime) is less than the fire time
					// This essentially shifts the time scale to the last firing cycle. If the number falls under the fire time, the 
					// turret is firing. return false in this case
					if (turretTurnsUntilFire(trt) < i){
						return true;
					}
				} catch(NoItemException e){
					// Do nothing, since no turret has been found
				}
			}
		}
		return false;
	}
	
	public int getMoveX(Direction d){
		int moveX = 0;
		if (d.equals(Direction.UP) || d.equals(Direction.DOWN)){
			moveX = 0;
		}
		else if (d.equals(Direction.RIGHT)){
			moveX = 1;
		}
		else {
			moveX = -1;
		}
		return moveX;
	}
	public int getMoveY(Direction d){
		int moveY = 0;
		if (d.equals(Direction.UP)){
			moveY = -1;
		}
		else if (d.equals(Direction.DOWN)){
			moveY = 1;
		}
		else if (d.equals(Direction.RIGHT)){
			moveY = 0;
		}
		else {
			moveY = 0;
		}
		return moveY;
	}
	
	public int handleWrapAroundY(int y, int moveY, Gameboard gameboard){
		int destY = 0;
		if (y + moveY >= gameboard.getHeight()){
			destY = y + moveY - gameboard.getHeight();
		}
		else if (y + moveY < 0){
			destY = y + moveY + gameboard.getHeight();
		}
		else {
			destY = y + moveY;
		}
		return destY;
	}
	
	public int handleWrapAroundX(int x, int moveX, Gameboard gameboard){
		int destX = 0;
		if (x + moveX >= gameboard.getWidth()){
			destX = x + moveX - gameboard.getWidth();
		}
		else if (x + moveX < 0){
			destX = x + moveX + gameboard.getWidth();
		}
		else {
			destX = x + moveX;
		}
		return destX;
	}
	
	public int turretTurnsUntilFire(Turret trt){
		if (totalTurns % (trt.getFireTime() + trt.getCooldownTime()) < trt.getFireTime()){
			//Turret is firing, return 0
			return 0;
		}
		else {
			// Breakdown
			// totalTurns % (trt.getFireTime() + trt.getCooldownTime()) 
			// - Represents the amount of time elapsed since last firing cycle (Fire time + cooldown time)
			// - This is subtracted from the firing cycle time to get the number of turns until next firing
			return (trt.getCooldownTime() + trt.getFireTime() - (totalTurns % (trt.getFireTime() + trt.getCooldownTime())));
		}
	}
	public boolean isTurretFiring(Turret trt){
		if (totalTurns % (trt.getFireTime() + trt.getCooldownTime()) < trt.getFireTime()){
			return true;
		}
		return false;
	}
	
	
}
