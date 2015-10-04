import java.util.ArrayList;

public class DecisionNode {
	ArrayList<DecisionNode> childNodes;
	CustomGameboard cboard;
	Move currentMove, bestMove;
	int turns;
	
	public DecisionNode(CustomGameboard cboard, Move currentMove, int turns) {
		this.turns = turns + 1;
		this.cboard = cboard;
		this.currentMove = currentMove;
		
		//update board
	}
	
	public void solve(int depth) {
		if (turns == depth) return;
		return;
	}
}
