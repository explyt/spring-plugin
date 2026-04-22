# Implementation Plan: explyt_trace_spring_call_chain

## Approach
Use UAST to walk method bodies and find UCallExpression → resolve to PsiMethod → get containingClass → detect Spring layer. Recurse up to `depth` levels. Optionally scan test files for references.

## Algorithm
1. Find the starting PsiMethod from filePath + line
2. Build chain recursively:
   - For each PsiMethod, find all UCallExpression in body → resolve to callee PsiMethods
   - Filter to only calls on Spring beans (injected fields, self-class methods)
   - Detect layer from containingClass stereotype
   - Recurse into each callee up to depth
3. If includeTests, for each discovered method run MethodReferencesSearch scoped to test sources

## Key design decisions
- The starting point is a file + line number (not a class name), matching how agents work
- The depth parameter caps recursion
- Internal methods (private methods in same class) are traced but labeled differently
- Non-Spring classes are included but labeled as "INTERNAL" or null layer
