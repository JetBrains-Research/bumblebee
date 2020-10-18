def main():
    for _ in range(10):
        if _ == 1:
            raise AssertionError()
            print(1)
            print(2)
            print(3)
        else:
            raise ArithmeticError()
            print(None)
            print(-1)
        print("Unreachable")
        print("None")
    print("Hello")
    return None
    print("World")


if __name__ == '__main__':
    main()

