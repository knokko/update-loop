# Update loop overview
Most games (and quite some other software) rely on some
kind of update method being called periodically with
the right frequency.

## Without explicit timing
The easiest way to implement an update loop is a `while`
loop:
```
while (shouldContinue) {
    update();
}
```
This approach is nice when it is beneficial to call the
update function as often as possible. For instance, it's
a quite sensible way to handle your render loop when
you don't care about power consumption, or when you
rely on something else to time your loops. For instance,
the graphics driver may do this in case of V-sync.

That having said, I would *not* recommend this approach
for updating game logic or physics since you normally
want this to have a fixed period.

## Busy waiting
A simple solution to maintain a fixed update period is
by using busy-waiting:
```
while (shouldContinue) {
    long startTime = System.nanoTime();
    update();
    while (System.nanoTime() < startTime + updatePeriod) {}
}
```
The advantage of this solution is that it is easy to
implement and probably very precise. The drawback is
that you can waste a lot of processor time and energy.
No other application on the computer can do anything
useful on the core that is waiting. This is especially
wasteful when the execution time of the update function
is much shorter than the update period.

## Sleeping
A simple alternative that avoids busy-waiting is
sleeping:
```
while (shouldContinue) {
    update();
    Thread.sleep(updatePeriod);
}
```
This is much better for power usage since the processor
uses much less power while sleeping. Also, other
applications (or other threads of your application)
can use the processor while you're sleeping. The
drawback is that it sleeps longer than it should,
increasing the update period. For instance, if your
update function takes 10 milliseconds and you sleep
20 milliseconds, the loop iteration will take 30
milliseconds in total. Fortunately, this is easy to solve:
```
while (shouldContinue) {
    long startTime = System.nanoTime();
    update();
    long updateTime = System.nanoTime() - startTime;
    long sleepTime = updatePeriod - updateTime / 1_000_000;
    if (sleepTime > 0) Thread.sleep(sleepTime);
}
```
I consider this as a decent solution, but it does have a
flaw: `Thread.sleep(x)` sleeps for **at least** *x*
milliseconds. The update period will be longer when it
sleeps longer. This basically causes 2 problems:
- The time between consecutive updates is not always the same.
- The *average* time between consecutive updates becomes
larger than the desired update period.

The first problem is basically impossible to solve on
personal computers, but the second problem can be solved.

## Smarter sleeping
The update period problem can be solved by remembering
the start time and the number of updates:
```
long startTime = System.nanoTime();
long updateCounter = 0;
while (shouldContinue) {
    long nextUpdateTime = startTime + updateCounter * updatePeriod;
    long sleepTime = (nextUpdateTime - System.nanoTime()) / 1_000_000;
    if (sleepTime > 0) Thread.sleep(sleepTime);
    update();
    updateCounter += 1;
}
```
This solution will ensure that the actual update period is
very close to the desired update period, *as long as the
average execution time of the update function* is smaller
than the desired update period. If the average execution time
is longer, the `sleepTime` will always be negative, hence
it is equivalent to `while (shouldContinue) { update(); }`.
Achieving the desired update period is obviously impossible
when the average execution time is larger than the update
period, so I consider this to be desired behavior.

As far as I know, this approach has only 1 potential
drawback. Consider the case when the update period is
50 milliseconds and the execution time of update is 60
milliseconds during a peak time of 50 minutes. Thereafter,
the update execution time is reduced to 10 milliseconds.
After the 50 minutes are over, the system will be 10 minutes
behind schedule, and try to catch up. To do so, it will
execute the update function as often as possible,
without sleeping, until it has caught up. This will cause
the update function to be called 100 times per second
for 2 minutes long (rather than the desired update
frequency of 20 times).

## Sliding window
Whether this behavior is desired, depends on the
application. For games, it probably isn't (imagine
all enemies suddenly running 5 times as fast
after lagging a while). The *smarter sleeping* approach
was made to solve the 'sleep jitter' problem, but the
smarter sleeping approach can be too extreme. Ideally,
you should use an approach that is more robust than 
the naive sleep approach, but is not as desperate as the
smarter sleeping approach. 

For instance, instead of counting the number of updates 
since the start of the application, you could count the 
number of updates the past second. When a single update
invocation takes too long, the system will simply cancel
or shorten the `sleepTime` of the next iteration (and
possibly affect some more iterations). When the execution
time of the update function is consistently larger than
the desired update period, the system won't sleep
because the number of updates per second is always smaller
than desired. When the execution time of the update
function suddenly drops, the system will try to compensate
for the *missed updates during the last second* rather
than all missed updates during the peak hours. Thus the
system will continue normal operation after at most 1
second.

## The `UpdateLoop` class
The `UpdateLoop` class of this library uses a sliding
window to track the number of updates that happened
during the past N updates. Both the update period and the
length of the sliding window are configurable. Together,
they determine how much time it 'looks back'