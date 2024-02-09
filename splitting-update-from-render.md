# Splitting the update loop from the render loop
Most games need to both update and render periodically.
There are basically 2 ways to accomplish this:
- Call both the update and render function in 1 loop
- Spawn 1 thread for the render loop and 1 for the update loop

## Using one loop
Using one loop is the easiest, and the easiest way to
implement this is basically:
```
while (shouldContinue) {
    update();
    render();
}
```

The drawback of this approach is that your update frequency
is tied to your framerate. The faster your computer, the
more updates you will get per second. All in-game characters
will literally move faster on faster computers.

### Using delta time for the update
A common 'solution' to this problem is adding a `deltaTime`
parameter to the update function:
```
long previousTime = System.currentTimeMillis();
while (shouldContinue) {
    long currentTime = System.currentTimeMillis();
    double deltaTime = (currentTime - previousTime) / 1000.0;
    previousTime = currentTime;
    update(deltaTime);
    render();
}
```
At least the speed of the in-game characters no longer
depends on the framerate, but it still has plenty of flaws
left:
1. It uses `System.currentTimeMillis()` instead of 
`System.nanoTime()`.
2. The update frequency is still tied to the framerate.
3. The physics is non-deterministic and its precision
depends on the framerate: lower framerate =
sloppy physics.

Problem 1 is easy to solve, but still relevant to point out.
The drawbacks of `System.currentTimeMillis()` are that it is
less precise than `System.nanoTime()`, and that it is
synchronized with the real time. The latter means that the
physics/update will go wild whenever the system time changes.
`System.nanoTime()` doesn't suffer from this problem.

Problem 2 is smaller than it used to be, but still not great.
To increase the framerate, both the update and render
performance need to be improved. Since the update performance
typically strongly depends on the CPU, buying a better
graphics card will only improve the framerate slightly.

Problem 3 is most visible when the framerate is low. When
the collision detection is simplistic, it even allows
characters to move through walls when the framerate is
sufficiently low (when 
`deltaTime * speed > characterWidth + wallWidth`). When
games have glitches that can only occur at low framerates,
this stupid update loop is probably the reason. This
particular bug can also be solved by using better
collision detection, but it won't solve the fundamental
problem of your non-deterministic physics: if two players
are in the same situation, the outcome will depend on
their framerate. For instance, some jumps could be
easier or harder if the framerate is higher or lower.

### Skipping physics based on time
An alternative to the `deltaTime` mess is using the
time to determine when physics should be skipped.
Something like this, for instance:
```
long lastUpdate = System.nanoTime();
while (shouldContinue) {
    long currentTime = System.nanoTime();
    if (currentTime - lastUpdate > updatePeriod) {
        lastUpdate = currentTime;
        update();
    }
    render();
}
```
Personally, I would consider this solution to be a lot
better than the `deltaTime` approach: the physics is
deterministic, and it's easier to achieve higher FPS
since you don't need to `update` between every `render`.
Still, there are 2 problems left:
1. When the framerate is low, the update frequency is also
low: all characters will be slower when the framerate is low.
2. Updating and rendering can't be done in parallel.

Problem 1 can be solved with a bit more effort, but
problem 2 is a fundamental problem when the update
loop and render loop are combined. Since almost any
somewhat-modern computer has at least 2 cores, this is
quite wasteful.

## Using separate loops
The aforementioned problems vanish when you use 
separate loops for rendering and updating. A simplistic
approach would be:
```
new Thread(() -> {
    while (shouldContinue) {
        update();
        maybeSleep();
    }
}).start();

while (shouldContinue) {
    render();
}
```
TODO Describe maybeSleep.
The advantages of this approach are:
- The update period can be chosen and is completely 
independent of the framerate.
- The application can update and render at the
same time (great for performance).

The drawback of this approach is that the
application can update and render at the same time.
All kinds of weird things and errors can occur when
the rendering is reading the game state while the
updater is changing it. For instance, a
`ConcurrentModificationException` could be thrown
if the rendering is reading the entity list while
the updater adds an entity. Generally, there are
countless consistency hazards that could go wrong.

### Synchronizing access to the game state
These (consistency) problems can be solved by
synchronizing on the game state (or some other
lock). A simple solution would be:
```
new Thread(() -> {
    while (shouldContinue) {
        synchronized(gameState) {
            update();
        }
        maybeSleep();
    }
}).start();

while (shouldContinue) {
    synchronized(gameState) {
        render();
    }
}
```
This solves all consistency problems, but it also has
a drawback: updating and rendering can't be done
in parallel, which beats one of the main purposes of 
splitting the loops in the first place...

### Copying the game state
Fortunately, this solution can be improved by using
smarting synchronization and some copies. For instance,
the renderer could copy the game state in the
synchronized block, after which it renders that copy
after the synchronized block:
```
new Thread(() -> {
    while (shouldContinue) {
        synchronized(gameState) {
            update();
        }
        maybeSleep();
    }
}).start();

while (shouldContinue) {
    GameState localGameState;
    synchronized(gameState) {
        localGameState = copy(gameState);
    }
    render(localGameState);
}
```
This solution allows the updater to update the game state
while the renderer is rendering a copy of it. This
is an improvement over the previous situation, but still
not perfect. For instance, when the renderer wants to
start its next frame, it needs to wait until the updater
finishes the current tick. There are plenty of ways to
solve this problem, but that's out of scope. The
purpose of this text was just to explain why splitting
the update and render loop is a good idea.