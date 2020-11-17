def main():
    if False:
        print(1)


def while_unreachable():
    while False:
        print(1)
        print(2)
        print(3)


def while_unreachable2():
    print("Hello")
    while False:
        print(1)
        print(2)
        print(3)
    else:
        print(1)


def if_unreachable_condition():
    print("World")
    if 4 == 5 or 11 + 2 == 0:
        print(1)
