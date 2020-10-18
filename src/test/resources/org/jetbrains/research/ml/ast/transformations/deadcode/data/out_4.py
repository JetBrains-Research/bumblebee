def main():
    for _ in range(10):
        if _ == 1:
            raise AssertionError()
        else:
            raise ArithmeticError()
    print("Hello")
    return None


if __name__ == '__main__':
    main()


