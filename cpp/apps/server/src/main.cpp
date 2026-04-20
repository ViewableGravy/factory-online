#include <iostream>
#include <string>

int main()
{
    std::cout << "Hello, world!" << std::endl;
    std::cout << "Enter text and press Enter: " << std::flush;

    std::string user_input;
    while (std::getline(std::cin, user_input)) {
        std::cout << "You entered: " << user_input << std::endl;
        std::cout << "Enter text and press Enter: " << std::flush;
    }

    return 0;
}