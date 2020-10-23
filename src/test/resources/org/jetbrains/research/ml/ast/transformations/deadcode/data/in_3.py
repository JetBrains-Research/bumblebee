def main():
    for _ in range(10):
        if _ == 1:
            raise AssertionError()
            print("Unreachable")
        else:
            raise ArithmeticError()
            print("Unreachable")
        print("None")
    print("Hello")
    return None
    print("World")
