import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GamePanel extends JPanel implements Runnable {
    int tileSize = 16; // Size of each tile
    int size = 50;
    int screenWidth = tileSize * (size + 2);
    int screenHeight = tileSize * (size + 2);

    /**Grid of tiles*/
    static Tile[][] grid;
    Thread gameThread;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.WHITE);
        this.setDoubleBuffered(true);

        // Initializes the grid of tiles
        grid = new Tile[size + 2][size + 2]; // Add 2 to each dimension for the border
        for (int i = 0; i < size + 2; i++) {
            for (int j = 0; j < size + 2; j++) {
                if (i == 0 || i == size + 1 || j == 0 || j == size + 1) {
                    Tile wallTile = new Tile(0, 0, 0);
                    wallTile.wall = true;
                    grid[i][j] = wallTile;
                    continue;
                }
                if ((i == 25 && j == 25) || (i == 26 && j == 25) || (i == 25 && j == 26) || (i == 26 && j == 26)) {
                    Tile t = new Tile(8, 8, 0.9);
                    t.prevVX = 10;
                    t.prevVY = 10;
                    t.prevdensity = 0.9;
                    grid[i][j] = t;
                    continue;
                }
                grid[i][j] = new Tile(0, 0, 0.1);
            }
        }
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouseInput(e);
            }
        });

        // Add MouseMotionListener for mouse dragging
        this.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseInput(e);
            }
        });
    }

    private void handleMouseInput(MouseEvent e) {
        int x = e.getX() / tileSize;
        int y = e.getY() / tileSize;
        grid[x][y].density = 0.8;

    }

    public void startThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / 60.0; // Nanoseconds per frame (60 FPS)
        long lastTime = System.nanoTime();
        long currentTime;
        double delta = 0;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                updateVelocities(delta);
                advectVY(delta);
                advectVX(delta);
                diffuse(delta);
                advectDensities(delta);
                stabilize(delta);
                repaint();
                delta = 0;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Render tiles
        for (int i = 0; i < size + 2; i++) {
            for (int j = 0; j < size + 2; j++) {
                Tile tile = grid[i][j];
                float normalizedPressure = (float) ((tile.pressure + 100 - (99.8)) / (99.8 - (100.2)));
                //float color = (240 - (normalizedPressure * 240)) / 360.0f;
                float color = (float)tile.density;
                g2.setColor(Color.getHSBColor(0, 0, color));
                if (tile.wall) {
                    g2.setColor(Color.getHSBColor(0, 0, 0));
                }
                g2.fillRect(i * tileSize, j * tileSize, tileSize, tileSize);

                // Draw border around each tile
                g2.setColor(Color.BLACK);
                g2.drawRect(i * tileSize, j * tileSize, tileSize, tileSize);
            }
        }

        // Render velocity vectors
        g2.setColor(Color.RED);
        for (int i = 1; i < size + 1; i++) {
            for (int j = 1; j < size + 1; j++) {
                Tile tile = grid[i][j];
                //Cell center coordinates
                int x1 = i * tileSize + tileSize / 2;
                int y1 = j * tileSize + tileSize / 2;
                //Arrow end coordinates
                int x2 = (int) (x1 + tile.VX * tileSize);
                int y2 = (int) (y1 + tile.VY * tileSize);
                drawArrow(g2, x1, y1, x2, y2);
            }
        }
    }


    public void advectVY(double delta) {
        for (int i = 1; i < grid.length - 1; i++) {
            for (int j = 1; j < grid.length - 1; j++) {
                double x = i - grid[i][j].VX * delta;
                double y = j - grid[i][j].VY * delta;
                x = Math.clamp(x, 2, grid.length - 2);
                y = Math.clamp(y, 2, grid.length - 2);

                double v00 = grid[Math.max(1, (int) Math.floor(x))][Math.max(1, (int) Math.floor(y))].VY;
                double v10 = grid[Math.min(grid.length - 1, (int) Math.ceil(x))][Math.max(1, (int) Math.floor(y))].VY;
                double v01 = grid[Math.max(1, (int) Math.floor(x))][Math.min(grid.length - 1, (int) Math.ceil(y))].VY;
                double v11 = grid[Math.min(grid.length - 1, (int) Math.ceil(x))][Math.min(grid.length - 1, (int) Math.ceil(y))].VY;


                double dx = x - Math.floor(x);
                double dy = y - Math.floor(y);

                double averageVY = (1 - dx) * (1 - dy) * v00 + dx * (1 - dy) * v10 + (1 - dx) * dy * v01 + dx * dy * v11;

                grid[i][j].VY = averageVY;
            }
        }
    }


    public void advectVX(double delta) {
        for (int i = 1; i < grid.length - 1; i++) {
            for (int j = 1; j < grid.length - 1; j++) {
                double x = i - grid[i][j].VX * delta;
                double y = j - grid[i][j].VY * delta;
                x = Math.clamp(x, 2, grid.length - 2);
                y = Math.clamp(y, 2, grid.length - 2);

                double v00 = grid[Math.max(1, (int) Math.floor(x))][Math.max(1, (int) Math.floor(y))].VX;
                double v10 = grid[Math.min(grid.length - 1, (int) Math.ceil(x))][Math.max(1, (int) Math.floor(y))].VX;
                double v01 = grid[Math.max(1, (int) Math.floor(x))][Math.min(grid.length - 1, (int) Math.ceil(y))].VX;
                double v11 = grid[Math.min(grid.length - 1, (int) Math.ceil(x))][Math.min(grid.length - 1, (int) Math.ceil(y))].VX;


                double dx = x - Math.floor(x);
                double dy = y - Math.floor(y);

                double averageVX = (1 - dx) * (1 - dy) * v00 + dx * (1 - dy) * v10 + (1 - dx) * dy * v01 + dx * dy * v11;

                grid[i][j].VX = averageVX;
            }
        }
    }


    public void advectDensities(double delta) {
        for (int i = 1; i < grid.length - 1; i++) {
            for (int j = 1; j < grid.length - 1; j++) {
                double x = i - grid[i][j].VX * delta;
                double y = j - grid[i][j].VY * delta;
                x = Math.clamp(x, 2, grid.length - 2);
                y = Math.clamp(y, 2, grid.length - 2);

                double d00 = grid[Math.max(1, (int) Math.floor(x))][Math.max(1, (int) Math.floor(y))].density;
                double d10 = grid[Math.min(grid.length - 1, (int) Math.ceil(x))][Math.max(1, (int) Math.floor(y))].density;
                double d01 = grid[Math.max(1, (int) Math.floor(x))][Math.min(grid.length - 1, (int) Math.ceil(y))].density;
                double d11 = grid[Math.min(grid.length - 1, (int) Math.ceil(x))][Math.min(grid.length - 1, (int) Math.ceil(y))].density;


                double dx = x - Math.floor(x);
                double dy = y - Math.floor(y);

                double averageDensity = (1 - dx) * (1 - dy) * d00 + dx * (1 - dy) * d10 + (1 - dx) * dy * d01 + dx * dy * d11;

                grid[i][j].density = averageDensity;
            }
        }
    }

    public void updateVelocities(double delta) {
        for (int i = 1; i < grid.length - 1; i++) {
            for (int j = 1; j < grid[0].length - 1; j++) {
                if (grid[i][j].wall) {
                    grid[i][j].VX = 0;
                    grid[i][j].VY = 0;
                    continue;
                }
                if ((i == 1 && j == 25) || (i == 2 && j == 25) || (i == 1 && j == 26) || (i == 2 && j == 26)) {
                    grid[i][j].VX += 0.2 * delta;
                    grid[i][j].VY += 0.8 * delta;
                }
            }
        }
    }

    public void stabilize(double delta) {
        double overrelax = 1.5; // Between 1 and 2
        int iterations = 600; // Number of iterations for pressure projection

        for (int k = 0; k < iterations; k++) {
            for (int i = 1; i < grid.length - 1; i++) {
                for (int j = 1; j < grid[0].length - 1; j++) {
                    // Calculate total divergence
                    double totalDivergence = (grid[i + 1][j].VX - grid[i][j].VX + grid[i][j + 1].VY - grid[i][j].VY);

                    int neighbors = 0;
                    if (!grid[i - 1][j].wall) {
                        neighbors++;
                    }
                    if (!grid[i][j - 1].wall) {
                        neighbors++;
                    }
                    if (!grid[i + 1][j].wall) {
                        neighbors++;
                    }
                    if (!grid[i][j + 1].wall) {
                        neighbors++;
                    }
                    double changeDiv = overrelax * (totalDivergence / neighbors);


                    // Update velocities
                    grid[i][j].VX += changeDiv;
                    grid[i][j].VY += changeDiv;
                    grid[i + 1][j].VX -= changeDiv;
                    grid[i][j + 1].VY -= changeDiv;

                    //Reflect of walls
                    if (grid[i - 1][j].wall && grid[i][j].VX < 0) {
                        grid[i][j].VX = -(grid[i][j].VX);
                        grid[i-1][j].VX = -(grid[i][j].VX);
                    }
                    if (grid[i][j - 1].wall && grid[i][j].VY < 0) {
                        grid[i][j - 1].VY = -(grid[i][j - 1].VY);
                        grid[i][j].VY = -(grid[i][j - 1].VY);
                    }
                    if (grid[i + 1][j].wall && grid[i + 1][j].VX > 0) {
                        grid[i + 1][j].VX = -grid[i + 1][j].VX;
                    }
                    if (grid[i][j + 1].wall && grid[i][j + 1].VY > 0) {
                        grid[i][j + 1].VY = -grid[i][j + 1].VY;
                    }
                    // Update pressure
                    grid[i][j].pressure += (totalDivergence / neighbors) * ((grid[i][j].density) / delta);
                }
            }
        }
    }

    public void diffuse(double delta) {
        // Diffusion coefficient
        double diffusion = delta * 0.0000000000001;
        for (int i = 1; i < grid.length - 1; i++) {
            for (int j = 1; j < grid[0].length - 1; j++) {
                // Walls have no density
                if (grid[i][j].wall) {
                    grid[i][j].density = 0;
                    continue;
                }
                // Calculate average density of neighbors
                int neighbors = 0;
                double neighborSum = 0;
                if (!grid[i - 1][j].wall) {
                    neighbors++;
                    neighborSum += grid[i - 1][j].prevdensity;
                }
                if (!grid[i][j - 1].wall) {
                    neighbors++;
                    neighborSum += grid[i][j - 1].prevdensity;
                }
                if (!grid[i + 1][j].wall) {
                    neighbors++;
                    neighborSum += grid[i + 1][j].prevdensity;
                }
                if (!grid[i][j + 1].wall) {
                    neighbors++;
                    neighborSum += grid[i][j + 1].prevdensity;
                }
                // Update density
                grid[i][j].density += (neighborSum - (neighbors * grid[i][j].density)) * diffusion;

            }
        }
        // Update previous densities
        for (Tile[] row : grid) {
            for (Tile t : row) {
                t.prevdensity = t.density;
            }
        }
    }

    // Helper method to draw an arrow
    private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2) {
        //Dieses 6 Zeilen sind von Stack Overflow, ich konnte aber bei erneuter recherche nicht den Beitrag finden
        int arrowSize = 5;
        int dx = x2 - x1, dy = y2 - y1;
        double angle = Math.atan2(dy, dx);
        g2.drawLine(x1, y1, x2, y2);
        g2.drawLine(x2, y2, x2 - (int) (arrowSize * Math.cos(angle - Math.PI / 6)), y2 - (int) (arrowSize * Math.sin(angle - Math.PI / 6)));
        g2.drawLine(x2, y2, x2 - (int) (arrowSize * Math.cos(angle + Math.PI / 6)), y2 - (int) (arrowSize * Math.sin(angle + Math.PI / 6)));
    }

}
