def main():
    for _ in range(10):
        if _ == 1:
            raise AssertionError()
        else:
            raise ArithmeticError()
        print("None")
    print("Hello")
    return None
    print("World")


if __name__ == '__main__':
    main()

