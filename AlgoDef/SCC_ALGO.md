# SCC Generation Algorithm

## Global
```
SCC_GRAPH G # final Index graph
```
## Check_Indexes

### Input
2. An array A containing a list of lines in a loop
2. An Array P of Phi Variables
3. A Hash Table H that maps Constants to values

```
for s in A:
    if s.contains_array_reference():
        index = s.get_Index()
        ar = s.get_array_reference()
        if !H.contains_key(index):
            T = s.get_read_or_write()
            N = node(s, ar, index, T, s.line_number())
            G.add_node(N)
```

## Get_SCC_Chain

### Input
1. Node n

```
L = new Array
L.add(n)
for x in G.get_nodes():
    if n.get_base_name() == x.get_base_name:
        if n.get_read_or_write() != x.get_read_or_write()
            L.add(x)
L.sort_by_line_number()
return L
```

## Make_SCC_Graph

### Input
2. An array A containing a list of lines in a loop
3. An Array P of Phi Variables
4. An Array Def/Use Graph R
5. A Hash Table H that maps Constants to values

```
Array Completed_Chains
Check_Indexes(A, P, H)
for c in G.get_nodes():
    scc_chain = Get_SCC_Chain(N)
    if scc_chain.length > 1 && !Completed_Chains.contains(scc_chain):
        for n in scc_chain:
            c_line = c.get_line()
            n_line = n.get_line()
            can_be_cycle = false
            same_line = false
            if c_line > n_line:
                if c.is_write() && n.is_read():
                    can_be_cycle = true
            else if c_line < n_line:
                if c.is_read() && n.is_write():
                    can_be_cycle = true
            else:
                can_be_cycle = true
                same_line = true
            if !can_be_cycle:
                G.add_basic_dep(c, n)
            else:
                Array d1 = get_variable_dependency_chain(c)
                Array d2 = get_variable_dependency_chain(n)
                eq1 = resolve_dep_chain_to_equation(d1)
                eq2 = resolve_dep_chain_to_equation(d2)
                Map r1
                Map r2
                result1 = solve(eq1, &r1)
                result2 = solve(eq2. &r2)
                if result1 == SAT && result2 == SAT:
                    d = result1.get_distance() - result2.get_distance()
                    G.add_dep(c, n)
                    if d != 0:
                        G.add_back_edge(n, c)
        completed_chains.add(scc_chain)
```

