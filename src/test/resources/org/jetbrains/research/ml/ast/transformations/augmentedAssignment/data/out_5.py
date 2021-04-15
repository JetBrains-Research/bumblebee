def f1():
    if 5 == 4:
        print("5 = 4")
    elif 4 == 3:
        print("4 = 3")
    f1_v1 = 4
    f1_v2 = 5


def f2():
    f2_v1 = 5
    f2_v2 = 6
    f2_v3 = 7
    if False:
        f2_v1 = f2_v2
        print(f2_v1)
    f2_v3 = f2_v3 + 3
    f2_v4 = f2_v2 = f2_v1 = f2_v3
    f1()
    f2_v4 = f2_v4 * (2 + 2)

    f2_v5 = int(input())
    if f2_v3 + f2_v5 > 14:
        print(f2_v3)
        print(f2_v5)
        f2_v4 = f2_v4 * (2 + 2)
    elif f2_v3 + f2_v4 > 15:
        print(f2_v4)
        print(f2_v5)
        f2_v4 = f2_v4 * (2 + 2)
    else:
        print(f2_v1)
        print(f2_v5)
        f2_v4 = f2_v4 * (2 + 2)

    if f2_v1 <= f2_v5 <= f2_v4 <= f2_v3:
        print(f2_v5)
    else:
        print("hello")

    if not ((f2_v1 and f2_v2) or (f2_v3 or f2_v4)):
        print(f2_v5)

    if f2_v1 < f2_v5 or f2_v2 <= f2_v3:
        print(f2_v4)


if __name__ == '__main__':
    f2()
