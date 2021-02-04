st = input()
if len(st) == 1:
    print(st)
elif len(st) % 2 == 1:
    print("(".join(st[:len(st) // 2]), "(", ")".join(st[len(st) // 2:]), sep="")
else:
    print("(".join(st[:len(st) // 2]), ")".join(st[len(st) // 2:]), sep="")
