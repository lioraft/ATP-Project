package Model;

import algorithms.mazeGenerators.Maze;
import algorithms.search.Solution;

public interface IModel {

    Maze generateMaze(int width, int height); // generate maze function
    Solution solveMaze(Maze maze); // solve maze function
}
