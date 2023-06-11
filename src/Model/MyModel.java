package Model;
import Client.IClientStrategy;
import IO.MyCompressorOutputStream;
import IO.MyDecompressorInputStream;
import Server.*;
import algorithms.mazeGenerators.Maze;
import Client.*;
import algorithms.mazeGenerators.Position;
import algorithms.search.AState;
import algorithms.search.Solution;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

/*
 * this class is the Model layer of the MVVM architecture.
 * it is responsible for the business logic of the application.
 * it implements the IModel interface.
 * it has a maze generating server and a solve search problem server.
 * it has a current maze and a character position.
 */
public class MyModel implements IModel{
    private Server mazeGeneratingServer; // maze generating server
    private Server solveSearchProblemServer; // solve search problem server
    private Maze currentMaze; // current maze
    private Solution solToMaze; // solution to current maze
    private Position characterPosition; // character position
    private int currentStep; // current step given in solution

    public MyModel() {
        // generate maze generating server
        mazeGeneratingServer = new Server(5400, 1000, new ServerStrategyGenerateMaze());
        // generate solve search problem server
        solveSearchProblemServer = new Server(5401, 1000, new ServerStrategySolveSearchProblem());
        // set all to null at first
        currentMaze = null;
        characterPosition = null;
        solToMaze = null;
        currentStep = 0;
    }

    // function to generate maze based on dimensions and return it.
    // once a maze is generated, it is set as the current maze.
    public void generateGame(int width, int height) {
        AtomicReference<Maze> mazeRef = new AtomicReference<>(); // Use AtomicReference to hold the maze object
        try {
            Client client = new Client(InetAddress.getLocalHost(), 5400, new IClientStrategy() {
                @Override
                public void clientStrategy(InputStream inFromServer, OutputStream outToServer) {
                    try {
                        ObjectOutputStream toServer = new ObjectOutputStream(outToServer);
                        ObjectInputStream fromServer = new ObjectInputStream(inFromServer);
                        toServer.flush();
                        int[] mazeDimensions = new int[]{width, height};
                        toServer.writeObject(mazeDimensions);
                        // send maze dimensions to server
                        toServer.flush();
                        byte[] compressedMaze = (byte[]) fromServer.readObject();
                        // read generated maze (compressed with MyCompressor) from server
                        InputStream is = new MyDecompressorInputStream(new ByteArrayInputStream(compressedMaze));
                        byte[] decompressedMaze = new byte[15000 /*giving max space*/];
                        // allocating byte[] for the decompressed maze
                        is.read(decompressedMaze); // fill decompressedMaze with bytes
                        Maze maze = new Maze(decompressedMaze);
                        mazeRef.set(maze); // set the maze object in the AtomicReference
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            client.communicateWithServer();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        // set current maze to the generated maze
        currentMaze = mazeRef.get(); // set the current maze
        // set starting position
        setCharacterPosition(currentMaze.getStartPosition().getRowIndex(), currentMaze.getStartPosition().getColumnIndex());
    }

    // function to solve maze and return solution
    public void solveGame() {
        if (currentMaze == null) {
            return;
        }
        AtomicReference<Solution> mazeSolution = new AtomicReference<>();
        try {
            Client client = new Client(InetAddress.getLocalHost(), 5401, new IClientStrategy() {
                @Override
                public void clientStrategy(InputStream inFromServer, OutputStream outToServer) {
                    try {
                        ObjectOutputStream toServer = new ObjectOutputStream(outToServer);
                        ObjectInputStream fromServer = new ObjectInputStream(inFromServer);
                        toServer.flush();
                        toServer.writeObject(currentMaze); // send maze to server
                        toServer.flush();
                        mazeSolution.set((Solution) fromServer.readObject()); // read generated maze (compressed with MyCompressor) from server
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            client.communicateWithServer();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        // set solution to current maze
        solToMaze = mazeSolution.get();
    }

    // function that saves character position in current maze
    public void setCharacterPosition(int row, int col) {
        if (currentMaze == null) {
            return;
        }
        // create character position and set it
        Position characterPosition = new Position(row, col);
        this.characterPosition = characterPosition;
    }

    // function that returns the current row index of the character
    // if there is no current maze, return -1
    public int getCurrentRow() {
        if (currentMaze == null) {
            return -1;
        }
        return characterPosition.getRowIndex();
    }

    // function that returns the current column index of the character
    // if there is no current maze, return -1
    public int getCurrentCol() {
        if (currentMaze == null) {
            return -1;
        }
        return characterPosition.getColumnIndex();
    }

    // function that returns the current maze
    public int[][] getCurrentMaze() {
    	return currentMaze.getMaze();
    }

    // function that returns the current maze start column
    public int getStartingCol() {
    	return currentMaze.getStartPosition().getColumnIndex();
    }

    // function that returns the current maze ending column
    public int getGoalCol() {
    	return currentMaze.getGoalPosition().getColumnIndex();
    }


    // function that returns the current maze rows number
    // if there is no current maze, return -1
    public int getMazeRows() {
        if (currentMaze == null) {
            return -1;
        }
    	return currentMaze.getMaze().length;
    }

    // function that returns the current maze columns number
    // if there is no current maze, return -1
    public int getMazeCols() {
        if (currentMaze == null) {
            return -1;
        }
    	return currentMaze.getMaze()[0].length;
    }

    // function that returns the current maze solution
    // if there is no current maze, return null
    public Solution getMazeSolution() {
        if (currentMaze == null) {
            return null;
        }
        if (solToMaze == null) {
            solveGame();
        }
        return solToMaze;
    }

    // function to get hint for the next solution step
    // search for current position in solution - if finds, return next step
    // if doesn't find, return solution next step from beginning
    public int[] getHint() {
        int[] nextStep = new int[2];
        // if there is no current maze, return -1
        if (currentMaze == null) {
            nextStep[0] = -1;
            nextStep[1] = -1;
            return nextStep;
        }
        // if there is game but no solution, solve maze
        if (solToMaze == null) {
            solveGame();
        }
        // get solution steps
        ArrayList<AState> solutionSteps = solToMaze.getSolutionPath();
        // get current position
        Position currentPos = new Position(characterPosition.getRowIndex(), characterPosition.getColumnIndex());
        int newStep = 0;
        // search for current position in solution
        for (int i = 0; i < solutionSteps.size()-1; i++) {
            if (solutionSteps.get(i).toString().equals(currentPos.toString())) {
                newStep = i;
            }
        }
        AState next;
        // if reached end of solution, return -1
        if (newStep >= solutionSteps.size() - 1 || currentStep >= solutionSteps.size() - 1) {
            nextStep[0] = -1;
            nextStep[1] = -1;
            return nextStep;
        }
        else {
            // if player is in right path, get next step
            if (newStep != 0) {
                currentStep = newStep;
            }
            // if player is not in right path, give him hint to next step from beginning (or since last asked hint)
            currentStep++;
            next = solutionSteps.get(currentStep);
        }
        // get next position in string format
        String nextPos = next.toString();
        // remove brackets from string
        if (nextPos.startsWith("{") && nextPos.endsWith("}")) {
            nextPos = nextPos.substring(1, nextPos.length() - 1);
        }
        // split string to get row and column indexes
        String[] index = nextPos.split(",");
        // convert to int and add to nextStep array
        nextStep[0] = Integer.parseInt(index[0]);
        nextStep[1] = Integer.parseInt(index[1]);
        // return next step
        return nextStep;
    }

    public void startServers() {
        // start server for generating mazes
        mazeGeneratingServer.start();
        // start server for solving mazes
        solveSearchProblemServer.start();
    }

    // function that stops the servers
    public void shutDownServers() {
        // stop servers
        mazeGeneratingServer.stop();
        solveSearchProblemServer.stop();
    }

    // function that saves maze to file
    public boolean saveCurrentMazeToFile(File file) {
        // if there is no current maze, return false
        if (currentMaze == null) {
            return false;
        }
        try {
            // create file output stream
            OutputStream out = new MyCompressorOutputStream(new FileOutputStream(file));
            // write compressed maze to file
            out.write(currentMaze.toByteArray());
            out.flush();
            out.close();
            // return true if succeeded
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            // return false if failed
            return false;
        }
    }

    public boolean loadCurrentMazeFromFile(File file) {
        try {
            // create bytes array for maze
            byte savedMazeBytes[];
            // create file input stream, decompress maze and read it
            InputStream in = new MyDecompressorInputStream(new FileInputStream(file));
            savedMazeBytes = new byte[10000];
            in.read(savedMazeBytes);
            in.close();
            // create new maze from bytes array
            currentMaze = new Maze(savedMazeBytes);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // function that checks if the given row and column are a wall
    public boolean isPassage(int row, int col) {
        if (currentMaze == null) {
            return false;
        }
        return currentMaze.isValidPassage(row, col);
    }

}
