# IndexSCC
## Global
```python
SCC_GRAPH G # final SCC graph
```
## Make_Index_SCC

### Input
1. An array A containing a list of lines in a loop
2. A Hash Table H that maps Constants to values
3. An Array SEEN of seen Values
4. An Array P of Phi Variables
```python
Array N # NODES

for line in A:
    index_name = line.get_array_reference().get_index()
    if !H.contains_key(index_name) && !SEEN.contains(index_name):
        G.add_node(index_name)

for node in N:
    Array D = get_variable_dependency_chain(node)
    eq = resolve_dep_chain_to_equation(D)
    Map S
    result = solve(eq, &S)
    if result == SAT:
        for pv in P:
            integer d = 0
            if(result.contains_key(pv)):
                d = result[pv]
            if !G.contains_node(pv):
                G.add_node(pv)
            G.add_edge(node, G[pv], d)
```
# VariableSCC:
## GLOBAL
```python
Array V # Parsed phi variables
SCC_GRAPH G # final SCC graph
```
## Make_Variable_SSS:
### Input
1. An Array P of Phi Variables  
2. A Hash Table H that maps Constants to values
3. An array A containing a list versioned of Array Variables
```python
for pv in P:
    Node current_node = pv
    add_phi_links(current_node, A, H)
```
## Add_Phi_Links:
### Input
1. An Array P of Phi Variables
2. A graph node C
3. A Hash Table H that maps Constants to values
4. An array A containing a list versioned of Array Variables
```python
G.add_node(C)
if !V.contains(C) && P.contains(C):
    handle_phi_variable(P, C, A, H)
else:
    hand_non_phi_variable(P, C, A, H)
```
## Handle_Phi_Variable
### Input
1. An Array P of Phi Variables
2. A graph node C
3. A Hash Table H that maps Constants to values
4. An array A containing a list versioned of Array Variables
```python
Array U = get_phi_var_args(P, C)
for use in U:
    if !G.contains_node(use)
        G.add_node(use)
    G.add_edge(C, use)
    if !H.contains_key(use):
        add_phi_links(P, use, A, H)
```
## Handle_Non_Phi_Variable:
### Input
1. An Array P of Phi Variables
2. A graph node C
3. A Hash Table H that maps Constants to values
4. An array A containing a list versioned of Array Variables
```python
Array D = get_variable_dependency_chain(C)
for dep in D:
    if !G.contains_node(dep)
        G.add_node(dep)
    G.add_edge(C, D)
    Array D1 = get_variable_decadency_chain(dep)
    for dep1 in D1:
        if !G.contains_node(dep1)
            G.add_node(dep1)
        G.add_edge(dep, dep1)
        if !H.contains_key(dep1) && !A.contains(dep1) && !is_number(dep1):
            add_phi_links(P, dep1, H, A)
```




