Dependencies
---
* Explain the dependencies and how I construct the def/use graph.

_EXPLAIN SCC_

_EXPLAIN SSA, AND IT'S IMPLICATIONS_

_EXPLAIN ARRAY SSA_

A program consists of many possibly dependent statements. A dependency is
any grouping of statements that share a common resource. 
<!--- Dependencies fall into two categories:  _definition_, _use_ (Def/Use dependency) and _read_, _write_ (R/W dependency). -->
Furthermore, every dependency can either effect
variables within the current loop iteration (Inter-Loop dependency) or
effect variables on a _different_ loop iteration (Cross-Loop dependency). 
The guarantees of SSA for non-array variables ensure that cross-loop 
dependencies _must_ involve array variables. This means that all 
inter-loop dependencies can be directly vectorized based on a basic
Def/Use graph. However, Cross-Loop dependencies require further analysis as 
well as the creation of a Strongly Connected Component Graph (SCC). 

To create an SCC, the original loop body is parsed to create three important
components. First is a list of constants along with values. The second
is a list of Phi variables (Induction Variables) coupled with their respective
_aliases_. Lastly, is a list of Def/Use dependencies for each alias.
Using these components, each alias can be defined by an equation that consists
of only Phi variables and constant values.   

_SHOW EXAMPLE_

The next step in SCC creation is to define each **node** of the graph. The parser
analyzes each statement of the loop. If the statement contains an array 
reference (Note, that due to SSA guarantees that there can be only one array 
reference in any single statement) it is classified as a **read**, or a **write**. 
The statement is a **write** only if the statement is an _assignment statement_, and 
the array reference is on the left-hand side of the assignment, otherwise it is classified 
as a **read**. This statement, its _original source_ line number, and index of the 
array reference are stored as a **node**.

After all the nodes have been found, edge creation and labeling begins. 
For each node, _c_, in the graph the following steps are taken:
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
        * If n.line_num > c.line_num and if n is a **read** while c is a **write**, a
            cycle cannot exist, and it is a simple dependency
        * If n.line_num > c.line_num and if n is a **write** while c is a **read**, 
            a cycle may exist that creates a cross-loop dependency
        * if n.line_num == c.line_num a cycle may exist that creates 
            a cross-loop dependency
    3. 
        1. Using the Phi variable list, and the Constant list the index variable 
            for each node is reduced to an equation consisting of _only_ phi 
            variables and constants.
        2. Using Z3 those equations are each solved. This is the distance between the 
            inductive Phi variable and the index. Subtracting the **read** d value from the 
            **write** d yields the final d value. 
    4. An Edge is created between c and n, with a label of d.
    5. If a cross-loop dependency exists then a back edge is created from n to c.

 