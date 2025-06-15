interface Animal {
    void makeSound();  // abstract method
}

class Dog implements Animal {
    public void makeSound() {
        System.out.println("Bark!");
    }
}

class Cat implements Animal {
    public void makeSound() {
        System.out.println("Meow!");
    }
}
public class App {
    public static void main(String[] args) {
        Animal a1 = new Dog();
        Animal a2 = new Cat();

        a1.makeSound();  // Output: Bark!
        a2.makeSound();  // Output: Meow!
    }
}
