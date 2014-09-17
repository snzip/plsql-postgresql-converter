package ru.barsopen.plsqlconverter.ast.typed;
public class savepoint_statement implements transaction_control_statements, _baseNode {
  public int _line = -1;
  public int _col = -1;
  public int _tokenStartIndex = -1;
  public int _tokenStopIndex = -1;
  public _baseNode _parent = null;
  public _baseNode _getParent() { return _parent; }
  public void _setParent(_baseNode value) { _parent = value; }
  public void _setBaseNode(_baseNode value) { this._parent = value; }
  public int _getLine() { return _line; }
  public int _getCol() { return _col; }
  public int _getTokenStartIndex() { return _tokenStartIndex; }
  public int _getTokenStopIndex() { return _tokenStopIndex; }
  public savepoint_name savepoint_name = null;
  public savepoint_name get_savepoint_name() { return this.savepoint_name; }
  public void set_savepoint_name(savepoint_name value) {
    if (this.savepoint_name != null) { this.savepoint_name._setParent(null); }
    this.savepoint_name = value;
    if (this.savepoint_name != null) { this.savepoint_name._setParent(this); }
  }

  public void _walk(_visitor visitor) {
    visitor.visit(this);
    if (this.savepoint_name != null) {
      this.savepoint_name._walk(visitor);
    }
  }
  public org.antlr.runtime.tree.Tree unparse() {
    org.antlr.runtime.CommonToken _token = new org.antlr.runtime.CommonToken(br.com.porcelli.parser.plsql.PLSQLParser.SAVEPOINT_VK);
    _token.setLine(_line);
    _token.setCharPositionInLine(_col);
    _token.setText("SAVEPOINT_VK");
    org.antlr.runtime.tree.CommonTree _result = new org.antlr.runtime.tree.CommonTree(_token);
    _result.setTokenStartIndex(_tokenStartIndex);
    _result.setTokenStopIndex(_tokenStopIndex);
    if (savepoint_name == null) { throw new RuntimeException(); }
    _result.addChild(savepoint_name.unparse());


    return _result;
  }

}
