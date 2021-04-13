def main(x):
    if x:
        print(1)
    elif False:
        print("unreachable")

    if False:
        print("Unreachable")

    while False:
        print("Unreachable")


def foo(x):
    if False:
        print("unreachable")
    elif x + 2 == 0:
        print(1)
        print(2)
    elif x + 4 == 0:
        print(1)
    elif False:
        print("Unreachable")

    if False:
        print("Unreachable")

    if False:
        print("Unreachable")