Dependencies
---

NOTES
----
* 2 types of dependence control-dependence and data-dependence.
* "Furthermore, every dependency can either effect variables within the current loop iteration 
        (intra-Loop dependency) or effect variables on a different loop iteration (Cross-Loop dependency)."
    * not correct. Define nodes in the graph, define the edges(show data-dependencies, and control dependency) 
            in the graph.
        * data dependency can be INTRA-loop dependencies or cross-loop dependencies.
        * LOOK up control dependency: if you have an 'if-then-else' statement the then is dependent on the if node etc.
* Explain the dependencies and how I construct the def/use graph.
* sum = sum + A[i] -> sum1 = phi(sum0,sum2); sum2 = sum1 + A[i]; For this case the d is statically 1 but 1 non-the-less 
    and is a cross-loop dep.
    * Do not Write it out but explain it. It is not interesting, but it is important. 
* Add control dependency easy.

<!--- Dependencies fall into two categories:  _definition_, _use_ (Def/Use dependency) and _read_, _write_ (R/W dependency). --->
<!---  This means that all inter-loop dependencies can be directly vectorized based on a basic Def/Use graph. ---> 

[THIS?](https://en.wikipedia.org/wiki/Strongly_connected_component)

_EXPLAIN SCC_

_EXPLAIN SSA, AND IT'S IMPLICATIONS_

_EXPLAIN ARRAY SSA_

A program consists of many possibly dependent statements. A dependency is
any grouping of statements that share a common resource. 
There are two types of dependencies that are considered. *Control-dependencies* are 
dependencies that are created by control flow nodes. The following describes these
dependencies for an if-then-else statement **word better**.
```java
if(<Cond1>) {
    <Stmt1>
} else if(<Cond2>) {
    <Stmt2>
} else {
    <Stmt3>
}
```
*data-dependencies* are dependencies that exist between nodes based on their interaction
with shared data. These are further broken down into two categories. 
Every data-dependency either effects variables within the current loop iteration 
(Inter-Loop dependency) or effects variables on a _different_ loop iteration 
(Cross-Loop dependency). One type of simple Cross-Loop dependency occurs
when a single variable is modified by a previous calculation of itself. This is
a trivial case, but still counts as a cross loop dependency with a static distance of 1
iteration between the definition and usage. _NAME HERE_  determines more complex 
cross-Loop dependencies through the creation and analysis and reasoning of a 
Strongly Connected Component Graph (SCC). 

<!--- To create an SCC graph, the original loop body is parsed to create three important
components. First is a list of constants along with values. The second
is a list of Phi variables (Induction Variables) coupled with their respective
_aliases_. Lastly, is a list of Def/Use dependencies for each alias.
Using these components, each alias can be defined by an equation that consists
of only Phi variables and constant values. (*TOO CODE LIKE*). --->

_SHOW EXAMPLE_
Each node of the graph represents a statement in a loop body, while 
each edge represents a dependency between the two connected nodes. [NEEDS BETTER EXPLANATION but way clearer]()
The parser analyzes each statement of the loop. If the statement contains an array 
reference (Note, that due to SSA guarantees that there can be only one array 
reference in any single statement) it is classified as a **read**, or a **write**. 
The statement is a **write** only if the statement is an _assignment statement_, and 
the array reference is on the left-hand side of the assignment, otherwise it is classified 
as a **read**. This statement, its _original source_ line number, and index of the 
array reference are stored as a **node**.

After all the nodes have been found, edge creation and labeling begins. 
For each node, _c_, in the graph the following steps are taken to determine the distance
between each pair of def/use statements. This value determines the parallelization schedule 
for each pair interaction in the context of the larger loop body:
1. Get a list, L, of all _other_ nodes that either **read** or **write** 
    to the same object as c.
2. For each node, n, in L
    1. Determine if n is the opposite dependency type as c 
        (read, write or write, read)  They cannot be the same.
    2. Determine what type of edge (if any should be added):
        * If n.line_num < c.line_num and if n is a **read** while c is a **write**, 
            a cycle may exist that creates a cross-loop dependency
        * If n.line_num < c.line_num and if n is a **write** while c is a **read**, 
            a cycle cannot exist, and it is a simple dependency
        * If n.line_num > c.line_num and if n is a **read** while c is a **write**, 
            a cycle cannot exist, and it is a simple dependency
        * If n.line_num > c.line_num and if n is a **write** while c is a **read**, 
            a cycle may exist that creates a cross-loop dependency
        * if n.line_num == c.line_num a cycle may exist that creates 
            a cross-loop dependency
    3. 
        1. Using the Phi variable list, and the Constant list the index variable 
            for each node is reduced to an equation consisting of _only_ phi 
            variables and constants.
        2. Using Z3 those equations are each solved. This is the distance between the 
            inductive Phi variable and the index. Adding the **read** d value and the 
            **write** d yields the final d value. 
    4. An Edge is created between c and n, with a label of d.
    5. If a cross-loop dependency exists then a back edge is created from n to c.

Once the SCC graph is complete we partition it into two parts. One partition is
the MPC sub-graph (original computation of loop) while the other is the index sub-graph 
(which is plain-text). By parsing the index sub-graph the individual components of every index can be 
placed in a single equation. This is how we determine the d values for every pair of Strongly connected
components. 

Once the analysis creates the SCC graph along with the d values, 
it applies known compiler optimizations on each def/use cycle to 
create a heuristic based schedule utilizing known compiler optimizations. In MPC 
identical operations can be infinitely amortized, avoiding the 
hard processor limit that restricts loop scheduling in HPC applications. 
Furthermore, operations on _secret_ values... (MORE HERE).

 Vectorization (WORK IN PROGRESS!)
 ---
 
_EXPLAIN ASSUMPTIONS_ Normally loop scheduling is an NPC-Complete problem but MPC provides....
 
After the creation of the SCC graph is complete, the *Vectorization* step takes place, scheduling independent 
operations in the most parallel way possible. The simple base case is _inter-loop_ dependencies. Take the following 
example which calculates the inner product of two arrays:
```java
int[] A = new int[N];
int[] B = new int[N];
int sum = 0;
for(int i = 0; i < A.length; i++) {
    sum += A[i] * B[i];
}
```
Each calculation of A[i] * B[i] is independent, as it does not involve any other member of the array.
This independence means that, given the proper resources, every multiplication can be done at once and then stored
in a temporary array. The summation of this temporary array is more complex, but only requires _log2(N)_ steps 
(again assuming adequate resources) as shown below. 

_SHOW DIAGRAM_

More generally,...




