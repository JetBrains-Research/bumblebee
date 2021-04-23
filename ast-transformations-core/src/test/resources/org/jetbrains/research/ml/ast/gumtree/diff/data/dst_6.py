s = input()
n = len(s)
is_odd = bool(n%2)
res = s[0]
for i in range(1, n//2):
    res +="("
    res += s[i]
    if is_odd:
        res  += '('

    for i in range(n//2,n-1):
        res += s[i]
    res += ")"
if n !=1:
    res += s[-1]
print(res)
