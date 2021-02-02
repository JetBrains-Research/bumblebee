def some_dead_code():
    if 5 == 4:
        print("5 = 4")
    elif 4 == 3:
        print("4 = 3")
    x = 4
    y = 5


def main():
    a = 5
    b = 6
    c = 7
    # Dead code
    if False:
        a = b
        print(a)
    # Augmented Assignment
    c += 3
    # Multiple Target Assignment
    d = b = a = c
    some_dead_code()
    """
    another Augmented Assignment
    """
    d *= 2 + 2

    e = int(input())

    # Multiple Operator Comparison
    if a <= e <= d <= c:
        print(e)
    else:
        print("hello")

    # Outer Not Elimination
    if not ((a and b) or (c or d)):
        print(e)

    # Comparison Unification
    if a < e or b <= c:
        print(d)


if __name__ == '__main__':
    main()
