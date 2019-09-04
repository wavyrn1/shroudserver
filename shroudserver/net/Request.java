package shroudserver.net;

public class Request {
    public enum Method {GET, EXIT, SEND, JOIN, CREATE, LEAVE};
    private Method method;
    private String argument;

    public Request(String request) throws InvalidRequestException {
        String[] fields = request.split(" ");
        String methodString = fields[0];
        if(fields.length == 1) {
            this.argument = "";
        }
        else {
            String buffer = "";
            for(int i = 1; i < fields.length; i++) {
                buffer += fields[i];
                buffer += " ";
            }
            buffer = buffer.substring(0, buffer.length() - 1);
            this.argument = buffer;
        }

        switch(methodString) {
            case "get": // no args
                this.method = Method.GET;
                break;
            case "exit": // no args
                this.method = Method.EXIT;
                break;
            case "send":
                this.method = Method.SEND;
                break;
            case "join":
                this.method = Method.JOIN;
                break;
            case "create":
                this.method = Method.CREATE;
                break;
            case "leave": // no args
                this.method = Method.LEAVE;
                break;
            default:
                throw new InvalidRequestException();
        }

        // check that if arguments are needed, they are there
        if((this.method != Method.GET && this.method != Method.EXIT && this.method != Method.LEAVE) && this.argument == "") {
            throw new InvalidRequestException();
        }

        // check that join has two arguments
        if(this.method == Method.JOIN && this.argument.split(" ").length != 2) {
            throw new InvalidRequestException();
        }
    }

    public Method getMethod() {
        return this.method;
    }

    public String getArgument() {
        return this.argument;
    }
}
